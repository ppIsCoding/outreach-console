/**
 * GitHub Pages 没有服务端路由回退，把 index.html 复制一份为 404.html，
 * 这样直接访问 /outreach-console/tasks 之类的路径也能正常加载 SPA。
 */
import { copyFileSync } from 'node:fs'

copyFileSync('dist/index.html', 'dist/404.html')
console.log('[pages] dist/404.html created')
