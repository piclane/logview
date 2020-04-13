import {TableColumn} from "element-ui/types/table-column";
import {Vue} from "vue-property-decorator";
import {VNode} from "vue";
import moment from "moment";
import {Mixin} from "vue-mixin-decorator";
import Path from "@/utils/Path";
import {File} from "@/utils/api/DirApiClient";

/**
 * ファイルテーブルのレンダラー用 mixin
 */
@Mixin
export class FileTableRenderers extends Vue {
    /**
     * 最終更新日時を現在時刻からの相対表現で描画する場合 true そうでない場合 false
     */
    private isRenderLastModifiedFromNow = true;

    public created(): void {
        this.$on('cell-click', (row: File, column: TableColumn) => {
            if(column.property === 'lastModified') {
                this.isRenderLastModifiedFromNow = !this.isRenderLastModifiedFromNow;
            }
        });
    }

    /**
     * ファイル名列を描画します
     *
     * @param record レコード
     * @param column 列
     * @param value 値
     */
    public renderName(this: Vue, record: File, column: TableColumn, value: File['name']): VNode | string {
        if (value) {
            const clazz = {
                    logfile: true,
                    readable: record.readable,
                    unreadable: !record.readable,
                    file: false,
                    dir: false
                },
                domProps = {
                    resource: this.$props.currentPath.resolve(Path.of(value)).normalize()
                },
                attrs = {
                    resource: '',
                    href: '',
                    title: record.readable ? '' : '読み込めません'
                },
                data = {class: clazz, attrs, domProps, on: {click: (e: MouseEvent) => { this.$emit('clickLink', e); }}},
                iconClazz = {
                    'icon-document': false,
                    'icon-folder': false,
                    'gray': !record.readable
                },
                baseUrl = process.env.BASE_URL.replace(/\/$/, '');
            switch(record.type) {
                case 'file':
                    attrs.href = `${baseUrl}/$${domProps.resource}`;
                    clazz.file = true;
                    iconClazz['icon-document'] = true;
                    break;
                case 'dir':
                    attrs.href = `${baseUrl}/@${domProps.resource}`;
                    clazz.dir = true;
                    iconClazz['icon-folder'] = true;
                    break;
            }
            if(!record.readable) {
                delete data.attrs.href;
                delete data.on.click;
            }
            const h = this.$createElement;
            return h('a', data, [
                h('i', {class: iconClazz}), value
            ]);
        }
        return '';
    }

    /**
     * 最終更新日時列を描画します
     *
     * @param record レコード
     * @param column 列
     * @param value 値
     */
    public renderLastModified(this: Vue, record: File, column: TableColumn, value: File['lastModified']): string {
        if (typeof value === "undefined") {
            return '';
        }
        if(this.$data.isRenderLastModifiedFromNow) {
            return moment(value).fromNow();
        } else {
            return moment(value).format('YYYY-MM-DD HH:mm:ss');
        }
    }

    /**
     * サイズ列を描画します
     *
     * @param record レコード
     * @param column 列
     * @param value 値
     */
    public renderSize(this: Vue, record: File, column: TableColumn, value: File['size']): string {
        if (typeof value === "undefined") {
            return '';
        } else if (value === 0) {
            return '0';
        }
        const sizes = ['  B', ' KB', ' MB', ' GB', ' TB'];
        const i = Math.floor(Math.log(value) / Math.log(1024));
        return (Math.floor(value / Math.pow(1024, i) * 10) / 10).toFixed(i === 0 ? 0 : 1) + sizes[i];
    }

    /**
     * パーミッション列を描画します
     *
     * @param record レコード
     * @param column 列
     * @param value 値
     */
    public renderPermission(this: Vue, record: File, column: TableColumn, value: File['permissions']): string {
        if(typeof value === 'undefined') {
            return '';
        }
        let result = [];
        result.push(value & 0x100 ? 'r' : '-');
        result.push(value & 0x80  ? 'w' : '-');
        result.push(value & 0x40  ? 'x' : '-');
        result.push(value & 0x20  ? 'r' : '-');
        result.push(value & 0x10  ? 'w' : '-');
        result.push(value & 0x8   ? 'x' : '-');
        result.push(value & 0x4   ? 'r' : '-');
        result.push(value & 0x2   ? 'w' : '-');
        result.push(value & 0x1   ? 'x' : '-');
        return result.join('');
    }
}
