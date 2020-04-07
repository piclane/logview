/**
 * キュー
 */
export default class Queue {
    /** キューが有効かどうか */
    private enabled = true;

    /** キューのタスクの最大容量 */
    private readonly capacity: number;

    /** 既にキューイング済みのタイマーID */
    private readonly timerIds: number[] = [];

    /**
     * コンストラクタ
     *
     * @param capacity タスクの最大容量
     */
    constructor(capacity: number) {
        this.capacity = capacity;
    }

    /**
     * キューにタスクを追加します
     * キューが無効の間はタスクは追加されません。
     *
     * @param fn キューに追加するタスク
     */
    public put(fn: Function): void {
        if(!this.enabled) {
            return;
        }
        const timerId = setTimeout(() => {
            const idx = this.timerIds.indexOf(timerId);
            if(idx !== -1) {
                this.timerIds.splice(idx, 1);
            }
            if(this.enabled) {
                fn();
            }
        }, 13);
        this.timerIds.push(timerId);

        // 容量を超えたタスクをキャンセル
        if(this.timerIds.length > this.capacity) {
            clearTimeout(this.timerIds.shift());
        }
    }

    /**
     * キューにある全てのタスクを取りやめ、キューを無効にします
     */
    public cancel(): void {
        this.enabled = false;
        this.clear();
    }

    /**
     * キューを全てクリアします
     */
    public clear(): void {
        this.timerIds.forEach(timerId => clearTimeout(timerId));
        this.timerIds.splice(0, this.timerIds.length);
    }

    /**
     * キューを再び有効にします
     */
    public resume(): void {
        this.enabled = true;
    }
}
