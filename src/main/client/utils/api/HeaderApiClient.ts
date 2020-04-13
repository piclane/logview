import axios from "axios";

/**
 * ヘッダの追加 HTML を取得します
 */
export function headerHtml(): Promise<string> {
    return axios.get(process.env.BASE_URL + 'api/header').then(resp => {
        return resp.data as string;
    });
}

/**
 * ヘッダを出力する非同期コンポーネント
 */
export const headerHtmlComponent = () => {
    return headerHtml().then(html => {
        return {
            template: `<div>${html}</div>`
        }
    })
};
