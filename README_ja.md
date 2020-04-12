# logview
新生代感覚ログビューアー

## 概要
logview アプリケーションは左右ペインに分かれたログビューアーです。
左ペインはファイルブラウザ、右ペインはログファイルの内容を表示します。

## 特徴
- 高速な動作  
  ファイル全体を読み込むことなく表示するので高速に動作します。
- ふつうの検索機能  
  1行ずつ検索を行います。
- かしこい検索機能  
  ログファイルの一つのイベントが複数行にまたがっていても、1つのログイベント全体を表示する事ができます。
- ログ表示の自動更新  
  サーバーサイドのログに追記されていても、追記された内容を表示し続けます。いわゆる `tail -f` です。
- 行ごとのパーマリンク  
  Github の様に行、もしくは行の範囲を選択してパーマリンクを作成できるので、ログの共有に便利です。
- 各種文字コードに対応  
  ファイルの文字コードを自動で認識します。つまりファイルは UTF-8 でなくても良いのです。

## スクリーンショット

![screenshot](https://raw.githubusercontent.com/piclane/logview/master/doc/screenshot.png)

### ヘッダー
アイコンと、ファイルブラウザのパスに応じたパンくずリストが表示されます。
また、この領域は context.xml を編集することでカスタマイズする事が可能です。

### ファイルブラウザ
context.xml に指定されたディレクトリをルートディレクトリとして、その内容が表示されます。
ルートディレクトリには任意のディレクトリを別途追加する事ができます。

ファイルブラウザのテーブルには以下の項目を表示することができます。
- ファイル名
- 最終更新日時
- ファイルサイズ
- 所有者
- グループ
- パーミッション

### ファイルビューアー
ファイルブラウザで選択したログファイルを表示します。

## 使用方法

### context.xml について

通常、 `web/META-INF/context.default.xml` を `web/META-INF/context.xml` にコピーして使用します。

context.xml の変数の値には Bash の様に `$VAL` `${VAL}` `${VAL:-foo}` `${VAL:+bar}` 形式で環境変数を使用することができます。
また、 `@/path/to/file` で外部のファイルの内容を参照できます。

#### context.xml の変数
- app/logview/rootDir  
  ルートディレクトリを指定します。
- app/logview/dirs/&lt;any dir name&gt;  
  ルートディレクトリの直下に配置する追加のディレクトリを指定します。
  これは複数記述する事ができます。
- app/logview/header  
  html を記述し、ヘッダ部に追加のアイコンなどを配置する事ができます。 `@/path/to/html` 形式で外部ファイルを読み込むことをお勧めします。
  html には ElementUI が使用可能で以下の様に記述する事ができます。
  ```html
  <el-button
    icon="el-icon-s-home"
    type="mini"
    @click="location.href = 'http://foobar.com/'"></el-button>
  ```

### ビルド

`web/META-INF/context.default.xml` を `web/META-INF/context.xml` にコピーし、設定を記述してビルドします。
ビルドには JDK8, nodejs, npm が必要です。

```bash
$ cd /path/to/logview
$ ./gradlew war
$ mv build/libs/*.war /path/to/tomcat-base/webapps/
```

### docker を使用する

今の所、ご自身で docker イメージをビルドする必要がありますが、docker 以外の環境を作る必要が無いので簡単です！

```bash
$ cd /path/to/logview
$ docker build -t logview .
$ docker run -d -v /path/to/local/logview/root/:/var/lib/logview/ --rm --name logview -p 8080:8080 logview:latest
```
