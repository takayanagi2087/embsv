# embsv.jar 簡易Webアプリケーションサーバ。
## Description

embsv.jarはまだ開発中です。

dataforms3.jarで作成したwarファイルを実行するには、Webサーバを用意し、
Apache Tomcat10と何らかのデータベースサーバを用意する必要があります。
この作業は専門知識が必要になるため、有用なwarファイルを公開しても
専門家でないと利用することができません。
そこでTomcat10 EmbeddedとJavaで作成されたデータベースであるApache Derby
を組み合わせた簡易的なWebアプリケーションサーバembsv.jarを作成しました。
embsv.jarを使うとwarファイル内に簡易Webアプリケーションサーバの機能を組み込むことが可能です。
Java21以上がインストールされた環境であれば以下のコマンドでWebアプリケーションサーバが起動します。

java -jar <embsv.jarを組み込んだwarファイル>

## Requirement

* Java21

## Licence
[MIT](https://github.com/takayanagi2087/dataforms/blob/master/LICENSE)

## Usage

【実行可能warファイルの作成】

```
以下のコマンドで実行可能なwarファイルを作成します。

java -jar embsv.jar -emb <入力warファイル> <出力warファイル>

このコマンドで処理した<出力warファイル>は以下のコマンドだけでWebサーバが起動します。
java -jar <出力warファイル> [option]

起動モード
-mode cmdline           Webサーバのみを起動します。GUIを使用しないモードです。
-mode tasktray          タスクトレイからWebサーバを制御したり、ブラウザを起動することができます。
-mode window            WindowからWebサーバを制御したり、ブラウザを起動することができます。

HTTPのサービスポート
-port <port>            HTTPのサービスポートを指定します。(デフォルト値:8080)

停止コマンドポート
-shutdownPort <port>    停止コマンドのサービスポートを指定します。(デフォルト値:8005)

ブラウザ起動
-browser                Webサーバの起動時にブラウザも起動し、Webアプリケーションをアクセスします。

サーバの停止
-stop                   Webサーバを停止します。

ヘルプ
-help                   この情報を表示します。
```

【Webアプリケーションの実行と停止】

```
以下のコマンドでWebアプリケーションを実行することができます。
java -jar embsv.jar -start <warファイル | Webアプリケーションパス>

以下のコマンドでWebアプリケーションを停止することができます。
java -jar embsv.jar -stop
```