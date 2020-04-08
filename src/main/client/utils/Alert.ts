import $ from "jquery";
import Swal, {SweetAlertResult} from "sweetalert2";

/**
 * データの読込に失敗した場合のアラートを表示します
 *
 * @param text メッセージ
 */
export function showLoadFailed(text: string): Promise<SweetAlertResult> {
    return Swal.fire({
        icon: 'error',
        title: 'データの読込に失敗しました',
        html: '<p></p>',
        onRender(modalElement: HTMLElement): void {
            $('p', modalElement)
                .text(text)
                .css({
                    whiteSpace: 'pre',
                    padding: '10px',
                    lineHeight: 1.5,
                    overflow: 'scroll',
                    textAlign: 'start',
                    fontSize: '11px',
                    fontFamily: 'monospace'
                });
        }
    });
}
