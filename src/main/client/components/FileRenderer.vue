<template>
    <div class="file-renderer logs">
        <div
            class="bof xof top"
            v-show="bof && !empty"
        ></div>
        <div
            class="show_before show_link top"
            v-show="!bof && !empty"
            @click="showBefore"
        ><s>Display previous lines</s></div>
        <div
            class="contents"
        ></div>
        <div
            class="show_after show_link bottom"
            v-show="!eof && !empty && !searching"
            @click="showAfter"
        ><s>Display next lines</s></div>
        <div
            class="eof xof bottom"
            v-show="eof && !empty"
        ><s>[EOF]</s></div>
        <div
            class="searching xof bottom"
            v-show="searching && !empty"
        ><s>[Searching<i class="icon-searching"></i>]</s></div>
    </div>
</template>

<script>
    import FileRendererViewModel from "@/components/FileRendererViewModel";

    export default {
        name: "file-renderer",
        data() {
            return {
                /** 初期状態の場合 true そうでない場合 false */
                empty: true,
                /** 先頭の行がファイルの先頭場合 true そうでない場合 false */
                bof: false,
                /** 末尾の行がファイルの末尾の場合 true そうでない場合 false */
                eof: false,
                /** 検索中の場合 true そうでない場合 false */
                searching: false,
                /** FileRendererViewModel */
                renderer: null
            };
        },
        props: {
            scrollLock: {
                type: Boolean,
                required: false,
                default: false
            },

        },
        created() {
            this.$on('show-tail', path => {
                if(path.isEmpty) {
                    return;
                }
                this.scrollLock = false;
                this.path = path;
                this.renderer.clear();
                this.renderer.openTail(path);
            });

            this.$on('show-head', path => {
                if(path.isEmpty) {
                    return;
                }
                this.scrollLock = false;
                this.path = path;
                this.renderer.clear();
                this.renderer.openHead(path);
            });

            this.$on('search', (query, smart) => {
                this.scrollLock = false;
                this.renderer.clear();
                this.renderer.search(
                    this.path,
                    query.split(/[\s\u3000]+/g).map(s => s.trim()),
                    smart);
            });
        },
        mounted() {
            this.renderer = new FileRendererViewModel(this);
        },
        destroyed() {
            this.renderer.destroy();
        },
        watch: {
            scrollLock: function(scrollLock) {
                if(scrollLock) {
                    this.renderer.suspend();
                } else {
                    this.renderer.resume();
                }
            },
            searching: function(searching) {
                this.$emit('searching', searching);
            }
        },
        methods: {
            showBefore: function() {
                this.renderer.showBefore();
            },
            showAfter: function() {
                this.renderer.showAfter();
            },
        }
    }
</script>

<style scoped>
    .logs {
        overflow: scroll;
        flex-grow: 1;
    }

    .logs > * {
        padding: 0 10px;
    }

    .logs > .top {
        padding-top: 10px;
    }

    .logs > .bottom {
        padding-bottom: 10px;
    }

    .logs > .xof {
        color: #227868;
    }

    .logs > .show_link {
        text-align: center;
        background-color: white;
        cursor: pointer;
        user-select: none;
        z-index: 1;
    }

    .logs > .show_before {
        padding: 10px 10px 0 10px;
        box-shadow: 0 0 20px 15px white;
    }

    .logs > .show_after {
        padding: 0 10px 10px 10px;
        box-shadow: 0 0 20px 15px white;
    }

    .logs > .show_link:hover {
        background-color: aliceblue;
    }

    .logs > .show_before:hover {
        box-shadow: 0 0 20px 15px aliceblue;
    }

    .logs > .show_after:hover {
        box-shadow: 0 0 20px 15px aliceblue;
    }

    .logs >>> s {
        display: block;
        height: 16px;
        line-height: 16px;
        font-size: 12px;
        font-family: monospace;
        white-space: pre;
        text-decoration: none;
    }

    .logs .contents {
        z-index: -1;
        position: relative;
    }

    .logs .contents >>> s.w2ui-marker {
        display: inline;
        color: #444;
        background-color: rgba(252, 244, 161, 0.48);
        font-weight: bold;
    }

    .logs .contents >>> span.error {
        padding: 10px;
        height: auto;
    }

    .logs .contents >>> span.error:before {
        content: 'エラー';
        display: block;
        border-bottom: 1px solid #CCC;
        padding: 5px 0;
        margin-bottom: 10px;
        font-size: 150%;
    }
</style>
<style src="../utils/Icon.css"></style>
