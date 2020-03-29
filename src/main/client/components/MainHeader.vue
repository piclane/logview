<template>
    <div class="main-header">
        <i class="logs"></i>
        <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.name" :to="item.path">
                <i v-if="item.home" class="el-icon-s-home"></i>
                <span v-else>{{ item.name }}</span>
            </el-breadcrumb-item>
        </el-breadcrumb>
        <slot></slot>
    </div>
</template>

<script>
    import Path from "@/utils/Path";

    export default {
        name: 'MainHeader',

        props: {
            currentPath: {
                type: Path,
                required: true
            }
        },

        computed: {
            /**
             * パンくずリストの要素を取得します
             *
             * @return {string[]} すべての パンくずリストの要素
             */
            breadcrumbItems: function() {
                const result = [{name: 'Home', path: '/', home: true}];
                for(let c of Array.from(this.currentPath.ancestor()).reverse()) {
                    result.push({
                        name: c.name(),
                        path: c.toString()
                    });
                }
                result[result.length - 1].path = '';
                return result;
            }
        }
    }
</script>

<style scoped>
    .main-header {
        position: relative;
        z-index: 100;
        padding: 0 24px;
        color: white;
        font-size: 12px;
        background: #000000;
        box-shadow: 0 0 5px 1px rgba(0, 0, 0, 0.5);
        height: 40px;
        line-height: 40px;
    }

    .main-header > * {
        vertical-align: middle;
        position: relative;
        top: -2px;
    }

    .main-header > .el-breadcrumb {
        margin-left: 10px;
        display: inline-block;
        cursor: default;
        user-select: none;
    }

    .main-header > .el-breadcrumb >>> .el-breadcrumb__inner {
        color: white;
    }

    .main-header > .el-breadcrumb .el-breadcrumb__item:last-child >>> .el-breadcrumb__inner {
        cursor: default !important;
    }

    .logs {
        background-image: url('../assets/log-24.png');
        background-position: center;
        display: inline-block;
        line-height: 24px;
        width: 24px;
        height: 24px;
        margin-right: 10px !important;
    }
</style>