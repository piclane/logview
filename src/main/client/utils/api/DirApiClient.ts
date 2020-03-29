import Path from "@/utils/Path";
import axios from "axios";

/**
 * ファイル情報
 */
export interface File {
    /** ファイル名 */
    name: string;

    /** サイズ (byte) */
    size: number;

    /** 最終更新日時 (エポックミリ秒) */
    lastModified: number;

    /** 所有者 */
    owner: string;

    /** グループ */
    group: string;

    /** ファイル種別 */
    type: 'file' | 'dir';

    /** パーミッション */
    permissions: number;

    /** 読込可能な場合 true そうでない場合 false */
    readable: boolean;
}

/**
 * 指定したパスに存在するすべてのファイルを取得します
 *
 * @param path パス
 */
export function listDir(path: Path): Promise<File[]> {
    return axios.post('api/files', new URLSearchParams({
        path: path.toString()
    })).then(resp => {
        return resp.data as File[];
    });
}
