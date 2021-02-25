# logview
新世代感覚ログビューアー

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
また、この領域は application.yml を編集することでカスタマイズする事が可能です。

### ファイルブラウザ
application.yml に指定されたディレクトリをルートディレクトリとして、その内容が表示されます。
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

### application.yml について

以下の様な内容で application.yml を作成し、 logview.jar と同階層に設置します。

```yaml
app:
  fs:
    root: ${HOME}
    dirs:
      log: /var/log
```

#### application.yml の変数
- app.fs.root  
  ルートディレクトリを指定します。
- app.fs.dirs  
  ルートディレクトリの直下に配置する追加のディレクトリを指定します。
  これは複数記述する事ができます。
- app.header  
  html を記述し、ヘッダ部に追加のアイコンなどを配置する事ができます。 
  html には ElementUI が使用可能で以下の様に記述する事ができます。
  ```html
  <el-button
    icon="el-icon-s-home"
    type="mini"
    @click="location.href = 'http://foobar.com/'"></el-button>
  ```

### ビルド

ビルドには JDK11, nodejs, npm が必要です。

```bash
$ cd /path/to/logview
$ ./gradlew bootJar
$ mv build/libs/*.jar /path/to/app/
```

### docker を使用する

今の所、ご自身で docker イメージをビルドする必要がありますが、docker 以外の環境を作る必要が無いので簡単です！

```bash
$ cd /path/to/logview
$ docker-compose build
$ docker-compose up -d
```
