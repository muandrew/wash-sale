// Read custom prefix from gradle properties / env
const fs = require('fs');
const path = require('path');

function getCustomPrefix() {
    try {
        const localPropsPath = path.resolve(__dirname, '../../../../local.properties');
        if (fs.existsSync(localPropsPath)) {
            const content = fs.readFileSync(localPropsPath, 'utf-8');
            for (const line of content.split('\n')) {
                const match = line.match(/^\s*wasm\.prefix\s*=\s*(.*?)\s*$/);
                if (match && match[1]) {
                    const val = match[1].replace(/^\/+|\/+$/g, '');
                    if (val) return '/' + val + '/';
                }
            }
        }
    } catch (e) {}

    if (process.env.WASM_PREFIX) {
        const val = process.env.WASM_PREFIX.replace(/^\/+|\/+$/g, '');
        if (val) return '/' + val + '/';
    }
    return '';
}

const prefix = getCustomPrefix();

// Allow reverse proxies / tunnel hosts (like Cloudflare Tunnel)
config.devServer = config.devServer || {};
config.devServer.allowedHosts = "all";

// Enable historyApiFallback so subpaths serve index.html
config.devServer.historyApiFallback = {
    index: '/index.html'
};

// Send strict no-cache headers to instruct Cloudflare & browsers to never cache dev server assets
config.devServer.headers = {
    "Cache-Control": "no-store, no-cache, must-revalidate, proxy-revalidate, max-age=0",
    "Pragma": "no-cache",
    "Expires": "0"
};

// If a prefix is configured, serve static assets (like .wasm files) at both / and the prefixed path
if (prefix) {
    config.output = config.output || {};
    config.output.publicPath = prefix;
    config.devServer.devMiddleware = config.devServer.devMiddleware || {};
    config.devServer.devMiddleware.publicPath = prefix;

    const defaultStatics = Array.isArray(config.devServer.static) 
        ? config.devServer.static 
        : (config.devServer.static ? [config.devServer.static] : ['kotlin']);

    config.devServer.static = [
        ...defaultStatics.map(s => (typeof s === 'string' ? { directory: s, publicPath: '/' } : s)),
        ...defaultStatics.map(s => (typeof s === 'string' ? { directory: s, publicPath: prefix } : { ...s, publicPath: prefix }))
    ];
}
