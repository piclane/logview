/**
 * キュー
 */
export default class Queue {
    /** キューが有効かどうか */
    private enabled = true;

    /** 既にキューイング済みのタイマーID */
    private timerIds = new Set<number>();

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
        let timerId = setTimeout(() => {
            this.timerIds.delete(timerId);
            if(!this.enabled) {
                return;
            }
            fn();
        }, 0);
        this.timerIds.add(timerId);
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
    }

    /**
     * キューを再び有効にします
     */
    public resume(): void {
        this.enabled = true;
    }
}
