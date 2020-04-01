import Path from "@/utils/Path";
import {WebSocketSource} from "@/utils/WebSocketSource";
import EventEmitter from "eventemitter3";


/** プロシージャ型 */
type Procedure = 'read' | 'search' | 'searchSmart';

/** 行の読込方向型 */
type Direction = 'forward' | 'backward';

/** 読込の開始位置型 */
type OffsetStart = 'head' | 'tail';

/**
 * 読込開始用のパラメーター
 */
export interface StartParam {
    /** ファイルパス */
    path: Path;

    /** プロシージャ */
    procedure: Procedure;

    /** 読み込む行数 (行) */
    lines: number;

    /**
     * 行の読込方向
     * - forward
     *   指定オフセットから順方向に 1 行ずつ読み込む
     * - backward
     *   指定オフセットから逆方向に 1 行ずつ読み込む
     */
    direction: Direction;

    /** 読込の開始位置 (byte) */
    offsetBytes: number;

    /**
     * 読込の開始位置のモード
     * - head
     *   指定されたオフセットがファイル先頭からの相対位置になる
     * - tail
     *   指定されたオフセットがファイル末尾からの相対位置になる
     */
    offsetStart: OffsetStart;

    /**
     * オフセット位置からスキップする行数
     * 正値が与えられた場合、末尾方向にスキップします
     * 負値が与えられた場合、先頭方向にスキップします
     * 0 の場合はスキップを行いません
     */
    skipLines: number;

    /**
     * ファイルが終端に達した後も監視を続けるかどうか
     * direction === 'forward' の場合のみ有効
     */
    follow: boolean;

    /**
     * 検索キーワード
     */
    search: string[];
}

/**
 * 行
 */
export interface Line {
    /** 文字列 */
    str: string;

    /** 先頭からの位置 (byte) */
    pos: number;

    /** 行長 (byte) */
    len: number;
}

/**
 * シグナル
 */
export interface Signal {
    /** シグナル名 */
    signal: string;

    /** 値 */
    value: any;
}

/**
 * メッセージ
 */
export type Message = Line | Signal;

/**
 * イベント名称とイベント型
 */
interface ProcedureApiClientMap<T> {
    "close": [CloseEvent, never];
    "error": [Event, never];
    "message": [Message[], T];
    "beforeStop": [never, never];
    "afterStop": [never, never];
    "beforeStart": [never, never];
    "afterStart": [never, never];
}

/**
 * ProcedureApiClient
 *
 * @param <T> 読込開始時の開始ユーザーオプションの型
 */
export class ProcedureApiClient<T extends {}> {
    /** WebSocketSource */
    private readonly wss: WebSocketSource;

    /** イベント発火するやつ */
    private readonly emitter = new EventEmitter();

    /** 読込開始時の開始パラメーター */
    private lastStartParam: StartParam = {} as StartParam;

    /** 読込開始時の開始ユーザーオプション */
    private lastStartOpioon: T = {} as T;

    /**
     * コンストラクタ
     */
    constructor() {
        this.wss = new WebSocketSource(ProcedureApiClient.createWsUrl(), ws => {
            ws.addEventListener('close', e => {
                this.dispatchEvent('close', e);
            });
            ws.addEventListener('message', e => {
                this.dispatchEvent('message', JSON.parse(e.data) as Message[], this.lastStartOpioon);
            });
        });
    }

    /**
     * インスタンスを破棄します
     */
    public destroy(): void {
        this.wss.getIfOpen().then(ws => {
            ws.send(JSON.stringify({status: 'stop'}));
            ws.close();
        }, () => {
            return;
        });
    }

    /**
     * 停止コマンドを送信します
     */
    public sendStop(): Promise<void> {
        return new Promise<void>(resolve => {
            const fn = (messages: Message[]) => {
                if(messages.length !== 1) {
                    return;
                }
                const signal = messages[0];
                if('signal' in signal && signal.signal === 'stopped') {
                    this.removeEventListener('message', fn);
                    resolve();
                }
            };

            this.dispatchEvent('beforeStop');
            this.addEventListener('message', fn);
            this.wss.getIfOpen().then(ws => {
                ws.send(JSON.stringify({
                    status: 'stop'
                }));
                this.dispatchEvent('afterStop');
            }, () => {
                this.removeEventListener('message', fn);
                resolve();
            });
        });
    }

    /**
     * 開始コマンドを送信します
     *
     * @param param 開始パラメーター
     * @param options ユーザーオプション
     */
    public sendStart(param: Partial<StartParam>, options?: T): void {
        this.dispatchEvent('beforeStart');

        this.lastStartOpioon = Object.assign({}, this.lastStartOpioon, options);
        this.lastStartParam = Object.assign({}, this.lastStartParam, param);
        const p = this.lastStartParam;
        this.wss.get().then(ws => {
            ws.send(JSON.stringify({
                status: 'start',
                path: p.path.toString(),
                procedure: p.procedure,
                direction: p.direction,
                lines: p.lines,
                offsetStart: p.offsetStart,
                offsetBytes: p.offsetBytes,
                skipLines: p.skipLines,
                follow: p.follow,
                search: p.search
            }));
            this.dispatchEvent('afterStart');
        })
    }

    /**
     * 停止後に開始コマンドを送信します
     *
     * @param param 開始パラメーター
     * @param options ユーザーオプション
     */
    public sendStopAndStart(param: Partial<StartParam>, options?: T): void {
        this.sendStop().then(() => {
            this.sendStart(param, options);
        });
    }

    /**
     * 最後に指定された走査方向
     */
    public get lastDirection(): Direction {
        return this.lastStartParam.direction;
    }

    /**
     * イベントを追加します
     *
     * @param type イベント名
     * @param listener イベントリスナー
     */
    public addEventListener<K extends keyof ProcedureApiClientMap<T>>(type: K, listener: (this: ProcedureApiClient<T>, e: ProcedureApiClientMap<T>[K][0], p: ProcedureApiClientMap<T>[K][1]) => any): void {
        this.emitter.addListener(type, listener, this);
    }

    /**
     * イベントを削除します
     *
     * @param type イベント名
     * @param listener イベントリスナー
     */
    public removeEventListener<K extends keyof ProcedureApiClientMap<T>>(type: K, listener: (this: ProcedureApiClient<T>, e: ProcedureApiClientMap<T>[K][0], p: ProcedureApiClientMap<T>[K][1]) => any): void {
        this.emitter.removeListener(type, listener, this);
    }

    /**
     * イベントを発火します
     *
     * @param type イベント名
     * @param args イベント
     */
    private dispatchEvent(type: string, ...args: any[]): boolean {
        return this.emitter.emit.apply(this.emitter, [type].concat(args) as [string, ...any[]]);
    }

    /**
     * WebSocket の URL を生成します
     *
     * @returns WebSocket の URL
     */
    private static createWsUrl(): string {
        const l = window.location,
            wsProtocol = l.protocol === 'https:' ? 'wss:' : 'ws:';
        return `${wsProtocol}//${l.host}${process.env.BASE_URL}api/procedure`;
    }
}
