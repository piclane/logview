/**
 * HTML 要素の指定文字をマークします
 */
export default class Marker {
    /**
     * マーカーのパターン
     */
    private readonly patterns: RegExp[];

    /**
     * コンストラクタ
     *
     * @param target マーク文字列
     */
    constructor(target: string[]) {
        this.patterns = target
            .map(str => str.replace(/[-[\]{}()*+?.,\\^$|#\s]/g, "\\$&").replace(/&/g, '&amp;').replace(/</g, '&gt;').replace(/>/g, '&lt;'))
            .map(str => new RegExp(str + '(?!([^<]+)?>)', "gi"));
    }

    /**
     * 指定された HTML 要素の一部をマークします
     *
     * @param el HTML 要素
     */
    public mark(el: HTMLElement): void {
        if(this.patterns.length === 0) {
            return;
        }
        let innerHTML = el.innerHTML, found = false;
        for(let p of this.patterns) {
            innerHTML = innerHTML.replace(p, matched => {
                found = true;
                return `<em>${matched}</em>`;
            });
        }
        if(found) {
            el.innerHTML = innerHTML;
        }
    }
}
