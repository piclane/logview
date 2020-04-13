import Path from "@/utils/Path";
import $ from 'jquery';

/**
 * ファイルをダウンロードします
 *
 * @param path ファイルのパス
 */
export function downloadFile(path: Path): void {
    if($('iframe.file-download').length === 0) {
        $('<iframe>')
            .addClass('file-download')
            .prop({
                name: 'download',
                src: "about:blank"
            })
            .css({
                position: 'absolute',
                top: 0,
                left: 0,
                width: 0,
                height: 0,
                border: 'none',
                padding: '0',
                margin: '0'
            })
            .appendTo('body');
    }

    window.open(process.env.BASE_URL + 'api/file/download?path=' + encodeURIComponent(path.toString()), 'download');
}
