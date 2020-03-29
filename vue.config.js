const path = require('path');

module.exports = {
    publicPath: '/logview/',
    devServer: {
        port: 18080,
        disableHostCheck: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                ws: true,
                changeOrigin: true
            }
        }
    },
    configureWebpack: {
        resolve: {
            alias: {
                '@': path.join(__dirname, '/src/main/client')
            }
        }
    },
    outputDir: 'src/main/webapp',
    pages: {
        index: {
            entry: 'src/main/client/index.ts',
            template: 'src/main/client/public/index.html',
            filename: 'index.html'
        },
        log: {
            entry: 'src/main/client/log.ts',
            template: 'src/main/client/public/log.html',
            filename: 'log.html'
        }
    }
};