import Queue from "@/utils/Queue";
import $ from "jquery";
import Path from "@/utils/Path";
import {Vue} from "vue/types/vue";
import {Line, Message, ProcedureApiClient, Signal, StartParam} from "@/utils/api/ProcedureApiClient";
import Marker from "@/utils/Marker";
import MouseMoveEvent = JQuery.MouseMoveEvent;
import MouseDownEvent = JQuery.MouseDownEvent;


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

    /** ロード中の場合 true そうでない場合 false */
    loading: boolean;
}

/**
 * 範囲
 */
interface Range {
    /**
     * 開始位置 (この位置を含む)
     */
    start: number;

    /**
     * 終了位置 (この位置を含む)
     */
    end: number;
}

/**
 * 開始モード
 */
interface StartMode {
    /**
     * スクロールモード
     * - top
     *   常にスクロールを上部に固定します
     * - bottom
     *   常にスクロールを下部に固定します
     * - keep
     *   表示が動かないように見えるようにスクロールを調節します
     * - none
     *   スクロールを制御しません
     */
    scroll: 'top' | 'bottom' | 'keep' | 'none';

    /**
     * 強調表示する範囲
     */
    emphasisRange?: Range;

    /**
     * 強調表示するマーカー
     */
    emphasisMarker?: Marker;
}

/** 1行の高さ */
const LINE_HEIGHT = 16;

/**
 * 選択ハンドルでマウスの左ボタンが押下された場合のハンドラーです
 */
class OnSelectionHandleMouseDown {
    /** div.contents 要素 */
    private readonly $parent: JQuery;

    /** 選択ハンドルの a 要素 */
    private readonly $start: JQuery;

    /** マウスが押下された時の clientY */
    private readonly startY: number;

    /** ファイルパス */
    private readonly path: string;

    /** 最後にマウスカーソルが移動したときの移動距離 */
    private lastDistance = 0;

    /** まだ 1 ブロック以上マウスカーソルが移動していない場合に true そうでない場合 false */
    private stable = true;

    /**
     * コンストラクタ
     *
     * @param e 選択ハンドルでマウスの左ボタンが押下された時の MouseDownEvent
     * @param path ファイルパス
     */
    constructor(e: MouseDownEvent, path: Path | string) {
        const $a = $(e.currentTarget);
        this.$parent = $a.closest('div');
        this.$start = $a.closest('s');
        this.startY = e.clientY;
        this.path = path.toString();
        $(document.body)
            .on('mousemove', this.onMouseMove.bind(this))
            .on('mouseup', this.onMouseUp.bind(this));
    }

    /**
     * mousemove イベントで呼び出されます
     *
     * @param e MouseMoveEvent
     */
    private onMouseMove(e: MouseMoveEvent): void {
        const $start = this.$start;
        const lastDistance = this.lastDistance;
        let distance = Math.floor((e.clientY - this.startY) / LINE_HEIGHT);
        if(lastDistance == distance) {
            return;
        }

        if(this.stable) {
            this.clearSelection();
            this.stable = false;
        }

        const minDistance = Math.min(lastDistance, distance);
        const maxDistance = Math.max(lastDistance, distance);

        for(let i=-1, $s=$start.prev(); i>=minDistance; i--, $s=$s.prev()) {
            if(i < maxDistance) {
                $s.toggleClass('select', distance < lastDistance);
            }
        }

        for(let i=1, $s=$start.next(); i<=maxDistance; i++, $s=$s.next()) {
            if(i > minDistance) {
                $s.toggleClass('select', distance > lastDistance);
            }
        }

        this.fillHref();

        this.lastDistance = distance;
    }

    /**
     * mouseup イベントで呼び出されます
     */
    private onMouseUp(): void {
        if(this.stable) {
            this.clearSelection();
            this.stable = false;
        }
        this.fillHref();
        $(document.body)
            .off('mousemove')
            .off('mouseup');
    }

    /**
     * 最初に選択された行を除いて、選択をすべて解除します
     */
    private clearSelection(): void {
        this.$parent.find('s.select')
            .children('a')
            .prop('href', function() { return $(this).data('href'); })
            .end()
            .removeClass('select');
        this.$start.addClass('select');
    }

    /**
     * 選択されたすべての行の href を更新します
     */
    private fillHref(): void {
        let top = -1, bottom = -1;
        this.$parent.find('s.select')
            .each(function() {
                const pos = $(this).data('pos');
                if(top === -1 || top > pos) {
                    top = pos;
                }
                if(bottom === -1 || pos > bottom) {
                    bottom = pos;
                }
            })
            .children('a')
            .prop('href', `#/$/file${this.path}#B${top}-${bottom}`);
    }
}


/**
 * FileRenderer の表示モデル
 */
export default class FileRendererViewModel {
    /** 表示される最大行数 */
    private static readonly bufferLines =
        process.env.NODE_ENV === 'development' ? 300 : 2000;

    /** ProcedureApi クライアント */
    private readonly client: ProcedureApiClient<StartMode>;

    /** タスクキュー */
    private readonly queue = new Queue(FileRendererViewModel.bufferLines / 10);

    /** 表示コンポーネントのデータ */
    private readonly data: ComponentData;

    /** <div class="file-renderer logs"> 要素 */
    private readonly $logs: JQuery;

    /** <div class="contents"></div> 要素 */
    private readonly $contents: JQuery;

    /** 表示されている行数 */
    private currentlineCount = 0;

    /** 中断中の場合 enabled:true そうでない場合 enabled:false */
    private suspended: {enabled: false} | {enabled: true; startMode: StartMode} = {enabled: false};

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
        this.client.addEventListener('message', (messages, startMode) => {
            this.queue.put(() => {
                this.receiveMessage(messages, startMode);
            });
        });
        this.client.addEventListener('beforeStart', () => {
            this.data.bof = false;
            this.data.eof = false;
        });

        this.data = component.$data as ComponentData;
        this.$logs = $(component.$el as HTMLElement);
        this.$contents = this.$logs.find('.contents')
            .on('click', 'a', e => {
                return e.shiftKey || e.metaKey;
            })
            .on('mouseenter', 'a', e => {
                $(e.target).closest('s').addClass('hover');
            })
            .on('mouseleave', 'a', e => {
                $(e.target).closest('s').removeClass('hover');
            })
            .on('mousedown', 'a', e => {
                if(e.button !== 0 || e.shiftKey || e.metaKey) {
                    return;
                }
                return new OnSelectionHandleMouseDown(e, this.client.lastParam('path'));
            })
            .on('dragstart', 'a', () => false);
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
     * @param params 開始パラメーター
     */
    public openHead(path: Path, params?: Partial<StartParam>): void {
        this.data.loading = true;
        this.client.sendStopAndStart(Object.assign({
            path: path,
            procedure: 'read',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'head',
            offsetBytes: 0,
            skipLines: 0,
            follow: true
        }, params), {
            scroll: "top"
        });
    }

    /**
     * ファイルの末尾を表示します
     *
     * @param path ファイルパス
     * @param params 開始パラメーター
     */
    public openTail(path: Path, params?: Partial<StartParam>): void {
        this.data.loading = true;
        this.client.sendStopAndStart(Object.assign({
            path: path,
            procedure: 'read',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'tail',
            offsetBytes: 0,
            skipLines: -FileRendererViewModel.bufferLines,
            follow: false
        }, params), {
            scroll: "bottom"
        });
    }

    public openThere(path: Path, range: Range): void {
        this.data.loading = true;
        this.client.sendStopAndStart({
            path: path,
            procedure: 'read',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'head',
            offsetBytes: range.start,
            skipLines: -Math.floor(FileRendererViewModel.bufferLines / 2),
            follow: true
        }, {
            scroll: "bottom",
            emphasisRange: range
        });
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
        this.client.sendStopAndStart({
            path: path,
            procedure: smart ? 'searchSmart' : 'search',
            lines: FileRendererViewModel.bufferLines,
            direction: 'forward',
            offsetStart: 'head',
            offsetBytes: 0,
            skipLines: 0,
            follow: true,
            search: query
        }, {
            scroll: "bottom",
            emphasisMarker: new Marker(query)
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
        this.data.loading = true;
        this.client.sendStopAndStart({
            lines: FileRendererViewModel.bufferLines / 2,
            direction: 'backward',
            offsetBytes: pos,
            offsetStart: 'head',
            skipLines: 0,
            follow: false
        }, {
            scroll: "keep"
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
        this.data.loading = true;
        this.client.sendStopAndStart({
            lines: FileRendererViewModel.bufferLines / 2,
            direction: 'forward',
            offsetBytes: pos,
            offsetStart: 'head',
            skipLines: 0,
            follow: !this.suspended
        }, {
            scroll: "keep"
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
        this.data.loading = false;
        this.$contents.empty();
        this.queue.resume();
        this.currentlineCount = 0;
    }

    /**
     * 表示の更新を一時停止します
     */
    public suspend(): void {
        this.suspended = {enabled: true, startMode: this.client.lastMode()};
        this.queue.cancel();
        this.client.sendStop().then(() => {
            this.queue.resume();
        });
    }

    /**
     * 表示の更新を再開します
     */
    public resume(): void {
        if(!this.suspended.enabled) {
            return;
        }
        const $lastLine = this.$contents.children('*:last');
        this.client.sendStart({
            offsetStart: 'head',
            offsetBytes: $lastLine.length ? $lastLine.data('pos') + $lastLine.data('len') : 0,
            skipLines: 0,
            follow: true
        }, this.suspended.startMode);
        this.suspended = {enabled: false};
    }

    /**
     * WebSocker の close イベント受信時に呼び出されます
     *
     * @param e CloseEvent
     */
    protected onClose(e: CloseEvent) {
        let nextPos = 0;
        this.data.loading = false;
        this.data.searching = false;
        this.$contents.children('s:last').each(function() {
            nextPos = $(this).data('pos') + $(this).data('len');
        });
        if(e.code !== 1000 /* 1000: Normal Closure */ && e.reason) {
            $('<s class="error"></s>')
                .text(e.reason)
                .data('pos', nextPos)
                .data('len', 0)
                .appendTo(this.$contents);
        } else {
            $('<s class="error"></s>')
                .text("An error has occurred while connecting to the server.")
                .data('pos', nextPos)
                .data('len', 0)
                .appendTo(this.$contents);
        }
    }

    /**
     * メッセージを処理します
     *
     * @param messages メッセージの配列
     * @param startMode 開始モード
     */
    protected receiveMessage(messages: Message[], startMode: StartMode): void {
        const emRange = Object.assign({
            start: -1, end: -1
        }, startMode.emphasisRange);
        let childCount = this.$contents.children().length;
        const path = this.client.lastParam('path').toString();
        const scrollTop = this.$logs.prop('scrollTop');
        let scrollDy = 0;
        for(let message of messages) {
            if('signal' in message) {
                this.processSignal(message, startMode);
                continue;
            }

            if('str' in message) {
                const line = message as Line;
                const href = `#/$/file${path}#B${line.pos}`;
                const $line = $('<s><a></a><b></b></s>')
                    .find('b')
                    .text(line.str)
                    .end()
                    .find('a')
                    .prop('href', href)
                    .end()
                    .toggleClass('emphasis', emRange.start <= line.pos && line.pos <= emRange.end)
                    .data({
                        href,
                        pos: line.pos,
                        len: line.len
                    });

                // 強調表示
                if(startMode.emphasisMarker) {
                    startMode.emphasisMarker.mark($line[0]);
                }

                // 配置
                childCount++;
                if (this.client.lastParam('direction') === 'forward') {
                    scrollDy -= LINE_HEIGHT;
                    this.$contents.append($line);
                    if (childCount > FileRendererViewModel.bufferLines) {
                        this.$contents.children('*:first').remove();
                        childCount--;
                    }
                } else {
                    scrollDy += LINE_HEIGHT;
                    this.$contents.prepend($line);
                    if (childCount > FileRendererViewModel.bufferLines) {
                        this.$contents.children('*:last').remove();
                        childCount--;
                    }
                }
            }
        }

        // 適切な位置にスクロールする
        if(scrollDy !== 0) {
            switch (startMode.scroll) {
                case "top":
                    this.$logs.scrollTop(0);
                    break;
                case "bottom":
                    this.$logs.scrollTop(this.$logs.prop('scrollHeight'));
                    break;
                case "keep":
                    this.$logs.scrollTop(scrollTop + scrollDy);
                    break;
            }
            this.data.loading = false;
        }
    }

    /**
     * シグナルを処理します
     *
     * @param signal シグナル
     * @param startMode 開始モード
     */
    protected processSignal(signal: Signal, startMode: StartMode): void {
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
            case 'eor':
                if(startMode.emphasisRange !== null) {
                    const $em = this.$contents.find('.emphasis:first');
                    if($em.length) {
                        const height = this.$logs.innerHeight() as number;
                        this.$logs.scrollTop($em.position().top - height / 4);
                        delete startMode.emphasisRange;
                    }
                }
                break;
        }
    }
}
