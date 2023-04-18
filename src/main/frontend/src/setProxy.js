// Spring Boot와 React 연동
// Proxy 설정
// npm install http-proxy-middleware --save
// 로 설치 후 다음과 같은 설정을 해준다.

const {createProxyMiddleware} = require('http-proxy-middleware');
module.exports = function (app) {
    app.use(
        '/api',
        createProxyMiddleware({
            target: 'http://localhost:8080', // 서버 URL 또는 localhost:사용포트
            changeOrigin: true,
        })
    );
};