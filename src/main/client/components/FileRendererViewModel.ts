import Queue from "@/utils/Queue";
import $ from "jquery";
import Path from "@/utils/Path";
import {Vue} from "vue/types/vue";
import {Line, Message, ProcedureApiClient, Signal, StartParam} from "@/utils/api/ProcedureApiClient";


/**
 * 表示コンポーネントのデータ型
 */
interface ComponentData {
    /** 初期状態の場合 true そうでない場合 false */
    empty: boolean;

    /** 先頭の行がファイルの先頭場合 true そうでない場合 false */
    bof: boolean;

    /** 末尾の行がファイルの末尾の場合 true そうでない場合 false */
    eof: boolean;

    /** 検索中の場合 true そうでない場合 false */
    searching: boolean;
}

/**
 * FileRenderer の表示モデル
 */
export default class FileRendererViewModel {
    /** 表示される最大行数 */
    private static readonly bufferLines =
        process.env.NODE_ENV === 'development' ? 100 : 2000;

    private readonly client: ProcedureApiClient;

    /** タスクキュー */
    private readonly queue = new Queue();

    /** 表示コンポーネントのデータ */
    private readonly data: ComponentData;

    /** <div class="file-renderer logs"> 要素 */
    private readonly $logs: JQuery;

    /** <div class="contents"></div> 要素 */
    private readonly $contents: JQuery;

    /** 表示されている行数 */
    private currentlineCount = 0;

    /** スクロール用のタイマーID */
    private scrollTimerId = -1;

    /**
     * コンストラクタ
     *
     * @param component Vue コンポーネント
     */
    constructor(component: Vue) {
        this.client = new ProcedureApiClient();
        this.client.addEventListener('close', e => {
            this.onClose(e);
        });
        this.client.addEventListener('message', messages => {
            this.queue.put(() => {
                this.receiveMessage(messages);
            });
        });
        this.client.addEventListener('beforeStart', () => {
            this.data.bof = false;
            this.data.eof = false;
        });

        this.data = component.$data as ComponentData;
        this.$logs = $(component.$el as HTMLElement);
        this.$contents = this.$logs.find('.contents');
    }

    /**
     * インスタンスを破棄します
     */
    public destroy(): void {
        this.client.destroy();
    }

    /**
     * ファイルの先頭を表示します
     *
     * @param path ファイルパス
     * @param options 開始パラメーター
     */
    public openHead(path: Path, options?: Partial<StartParam>): void {
        this.client.sendStop();
        this.client.sendStart(Object.assign({
            path: path,
            procedure: 'read',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'head',
            offsetBytes: 0,
            skipLines: 0,
            follow: true
        }, options));
    }

    /**
     * ファイルの末尾を表示します
     *
     * @param path ファイルパス
     * @param options 開始パラメーター
     */
    public openTail(path: Path, options?: Partial<StartParam>): void {
        this.client.sendStop();
        this.client.sendStart(Object.assign({
            path: path,
            procedure: 'read',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'tail',
            offsetBytes: 0,
            skipLines: -FileRendererViewModel.bufferLines,
            follow: true
        }, options));
    }

    /**
     * ファイル内検索を行います
     *
     * @param path ファイルパス
     * @param query クエリ文字列
     * @param smart かしこい検索を有効にする場合 true そうでない場合 false
     */
    public search(path: Path, query: string[], smart: boolean): void {
        this.data.searching = true;
        this.client.sendStop();
        this.client.sendStart({
            path: path,
            procedure: smart ? 'searchSmart' : 'search',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'head',
            offsetBytes: 0,
            skipLines: 0,
            follow: true,
            search: query
        });
    }

    /**
     * 表示中の先頭行より前の行を表示します
     */
    public showBefore(): void {
        if(this.data.bof) {
            return;
        }

        const pos = this.$contents.children('*:first').data('pos');
        this.client.sendStop();
        this.client.sendStart({
            lines: FileRendererViewModel.bufferLines / 2,
            direction: 'backward',
            offsetBytes: pos,
            offsetStart: 'head',
            skipLines: 0,
            follow: false
        });
    }

    /**
     * 表示中の末尾行より後の行を表示します
     */
    public showAfter(): void {
        if(this.data.eof) {
            return;
        }

        const $last = this.$contents.children('*:last');
        const pos = $last.data('pos') + $last.data('len');
        this.client.sendStop();
        this.client.sendStart({
            lines: FileRendererViewModel.bufferLines / 2,
            direction: 'forward',
            offsetBytes: pos,
            offsetStart: 'head',
            skipLines: 0,
            follow: false
        });
    }

    /**
     * 画面をクリアします
     */
    public clear(): void {
        this.data.empty = true;
        this.data.bof = false;
        this.data.eof = false;
        this.data.searching = false;
        this.$contents.empty();
        this.queue.resume();
        this.currentlineCount = 0;
    }

    /**
     * 表示の更新を一時停止します
     */
    public suspend(): void {
        this.queue.cancel();
        this.client.sendStop();
    }

    /**
     * 表示の更新を再開します
     */
    public resume(): void {
        this.queue.resume();
        const $lastLine = this.$contents.children('*:last');
        this.client.sendStart({
            offsetStart: 'head',
            offsetBytes: $lastLine.length ? $lastLine.data('pos') + $lastLine.data('len') : 0,
            skipLines: 0
        });
    }

    /**
     * WebSocker の close イベント受信時に呼び出されます
     *
     * @param e CloseEvent
     */
    protected onClose(e: CloseEvent) {
        let nextPos = 0;
        this.$contents.children('span:last').each(function() {
            nextPos = $(this).data('pos') + $(this).data('len');
        });
        if(e.code !== 1000 /* 1000: Normal Closure */) {
            $('<span class="eof error"></span>')
                .text(e.reason)
                .data('pos', nextPos)
                .data('len', 0)
                .appendTo(this.$contents);
        } else {
            $('<span class="eof"></span>')
                .data('pos', nextPos)
                .data('len', 0)
                .appendTo(this.$contents);
        }
    }

    /**
     * メッセージを処理します
     *
     * @param messages メッセージの配列
     */
    protected receiveMessage(messages: Message[]): void {
        let childCount = this.$contents.children().length;
        for(let message of messages) {
            if('signal' in message) {
                this.processSignal(message);
                continue;
            }

            if('str' in message) {
                const line = message as Line;
                const $line = $('<s>')
                    .text(line.str)
                    .data({
                        pos: line.pos,
                        len: line.len
                    });

                childCount++;
                if (this.client.lastDirection === 'forward') {
                    this.$contents.append($line);
                    if (childCount > FileRendererViewModel.bufferLines) {
                        this.$contents.children('*:first').remove();
                    }
                    this.scrollToBottom();
                } else {
                    this.$contents.prepend($line);
                    if (childCount > FileRendererViewModel.bufferLines) {
                        this.$contents.children('*:last').remove();
                    }
                }
            }
        }
    }

    /**
     * シグナルを処理します
     *
     * @param signal シグナル
     */
    protected processSignal(signal: Signal): void {
        switch(signal.signal) {
            case 'file_length':
                if(signal.value) {
                    this.data.empty = false;
                }
                break;
            case 'bof':
                this.data.bof = true;
                break;
            case 'eof':
                this.data.eof = true;
                this.data.searching = false;
                break;
        }
    }

    /**
     * 末尾にスクロールします
     */
    private scrollToBottom(): void {
        if(this.scrollTimerId < 0) {
            this.scrollTimerId = setTimeout(() => {
                this.$logs.scrollTop(this.$logs.prop('scrollHeight'));
                this.scrollTimerId = -1;
            }, 10);
        }
    }
}
