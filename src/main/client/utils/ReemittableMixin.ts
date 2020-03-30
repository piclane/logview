import {Mixin} from "vue-mixin-decorator";
import {Vue} from "vue-property-decorator";

/**
 * イベントを再放出可能にする為のミックスイン
 */
@Mixin
export default class ReemittableMixin extends Vue {
    /**
     * イベントを再放出します
     * 使用する場合は以下の様に記述します
     * ```
     * <some-component
     *     @click="passthroughEvent('click', arguments)">
     * </some-component>
     * ```
     *
     * @param name イベント名
     * @param args arguments を指定します
     * @param target イベントの送信先。省略すると自分自身になります
     */
    public passthroughEvent(name: string, args: ArrayLike<any>, target?: Vue): void {
        if(typeof target === 'undefined') {
            target = this;
        }

        // @ts-ignore
        target.$emit.apply(target, [name].concat(Array.from(args)));
    }


}
