/**
 * パスを表現します
 */
export default class Path implements Iterable<string> {
    /** ルートを表す Path */
    private static ROOT_PATH = new Path([], true);

    /** 空のパス */
    private static EMPTY_PATH = new Path([], false);

    /** パスの要素の配列 */
    private readonly components: string[];

    /** この Path が絶対パスの場合 true 相対パスの場合 false */
    public readonly isAbsolute: boolean;

    /**
     * 空の Path を取得します
     */
    public static empty(): Path {
        return Path.EMPTY_PATH;
    }

    /**
     * パスを表現する文字列から Path を生成します
     *
     * @param path パスを表現する文字列
     */
    public static of(path: string): Path {
        path = path.replace(/[/]+/g, '/').replace(/(^\s*)|([/]+\s*$)/g, '');
        if(path === '') {
            return Path.EMPTY_PATH;
        } else if(path === '/') {
            return Path.ROOT_PATH;
        } else if(path[0] === '/') {
            return new Path(path.substr(1).split(/[/]+/), true);
        } else {
            return new Path(path.split(/[/]+/), false);
        }
    }

    /**
     * コンストラクタ
     *
     * @param components パスの要素の配列
     * @param isAbsolute この Path が絶対パスの場合 true 相対パスの場合 false
     */
    private constructor(components: string[], isAbsolute: boolean) {
        this.components = components;
        this.isAbsolute = isAbsolute;
    }

    /**
     * この Path がルートパスの場合 true そうでない場合 false
     */
    public get isRoot(): boolean {
        return this.isAbsolute && this.components.length === 0;
    }

    /**
     * この Path が空のパスの場合 true そうでない場合 false
     */
    public get isEmpty(): boolean {
        return !this.isAbsolute && this.components.length === 0;
    }

    /**
     * 親のパスが存在する場合 true そうでない場合 false
     */
    public get hasParent(): boolean {
        return this.components.length > 0;
    }

    /**
     * 要素中の文字列を取得します
     *
     * @param index 要素のインデックス。省略すると最後の要素を取得します。
     */
    public name(index?: number): string {
        const cs = this.components;
        if(typeof index === 'undefined') {
            index = cs.length - 1;
        }
        return cs.length > 0 ? cs[index] : '';
    }

    /**
     * 指定された Path をこの Path に対して解決します
     *
     * @param other この Path に対して解決するパス
     */
    public resolve(other: Path): Path {
        if(other.isAbsolute) {
            return other;
        }
        return new Path(this.components.concat(other.components), this.isAbsolute);
    }

    /**
     * このパスから冗長な名前要素を削除したパスを返します
     */
    public normalize(): Path {
        const result: string[] = [];
        for(let c of this.components) {
            if(c === '.') {
                // nop
            } else if(c === '..' && result.length > 0) {
                result.pop();
            } else {
                result.push(c);
            }
        }
        return new Path(result, this.isAbsolute);
    }

    /**
     * 親の Path を取得します
     */
    public parent(): Path {
        if(this.isRoot) {
            return Path.ROOT_PATH;
        } else if(this.isEmpty) {
            return Path.EMPTY_PATH;
        } else {
            return new Path(this.components.slice(0, -1), this.isAbsolute);
        }
    }

    /**
     * 祖先のパスをこのパスから順に取得する IterableIterator を取得します
     */
    public *ancestor(): IterableIterator<Path> {
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        for(let c: Path = this; c.hasParent; c = c.parent()) {
            yield c;
        }
    }

    /**
     * この Path に含まれる要素の Iterator を取得します
     */
    [Symbol.iterator](): Iterator<string> {
        return this.components[Symbol.iterator]()
    }

    /**
     * この Path の文字列表現を取得します
     */
    public toString(): string {
        let result = this.components.join('/');
        if(this.isAbsolute) {
            result = '/' + result;
        }
        return result;
    }
}