/**
 * WebSocket を安全に利用する為のクラス
 */
export class WebSocketSource {
    /** WebSocket の接続先 URL */
    private readonly url: string;

    /** WebSocket の初期化関数 */
    private readonly initFn: (ws: WebSocket) => void;

    /** WebSocket */
    private ws: WebSocket | null = null;

    /**
     * コンストラクタ
     *
     * @param url WebSocket の接続先 URL
     * @param initFn WebSocket の初期化関数
     */
    constructor(url: string, initFn: (ws: WebSocket) => void) {
        this.url = url;
        this.initFn = initFn;
    }

    /**
     * 接続済みの WebSocket を取得します
     */
    public get(): Promise<WebSocket> {
        if (this.ws === null ||
            this.ws.readyState === WebSocket.CLOSED ||
            this.ws.readyState === WebSocket.CLOSING) {
            return new Promise<WebSocket>((resolve, reject) => {
                const ws = this.ws = new WebSocket(this.url);
                ws.addEventListener('open', e => {
                    resolve(e.target as WebSocket);
                });
                ws.addEventListener('error', e => {
                    reject(e);
                });
                this.initFn(ws);
            });
        } else if (this.ws.readyState === WebSocket.CONNECTING) {
            const ws = this.ws as WebSocket;
            return new Promise<WebSocket>((resolve, reject) => {
                ws.addEventListener('open', e => {
                    resolve(e.target as WebSocket);
                });
                ws.addEventListener('error', e => {
                    reject(e);
                });
            });
        } else {
            const ws = this.ws as WebSocket;
            return new Promise<WebSocket>(resolve => {
                resolve(ws);
            });
        }
    }

    /**
     * WebSocket が接続済みの時のみ resolve される Promise を取得します
     */
    public getIfOpen(): Promise<WebSocket> {
        if (this.ws !== null &&
            this.ws.readyState === WebSocket.OPEN) {
            const ws = this.ws as WebSocket;
            return new Promise<WebSocket>(resolve => {
                resolve(ws);
            });
        } else {
            return new Promise<WebSocket>(() => {});  // eslint-disable-line
        }
    }
}
