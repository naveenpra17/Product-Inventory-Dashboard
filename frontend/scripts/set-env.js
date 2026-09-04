const fs = require('fs');
const path = require('path');

const apiUrl = process.env.API_URL || 'http://localhost:8080/api';

if (!apiUrl) {
  console.error('API_URL environment variable is required for production builds.');
  process.exit(1);
}

const normalizedApiUrl = apiUrl.endsWith('/api')
  ? apiUrl
  : apiUrl.endsWith('/')
    ? `${apiUrl}api`
    : `${apiUrl}/api`;

const content = `export const environment = {
  production: true,
  apiUrl: '${normalizedApiUrl}'
};
`;

const target = path.join(__dirname, '..', 'src', 'environments', 'environment.prod.ts');
fs.writeFileSync(target, content, 'utf8');
console.log(`Wrote production API URL: ${normalizedApiUrl}`);
