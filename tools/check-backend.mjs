import { spawnSync } from 'node:child_process';
import { mkdirSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const offline = process.argv.slice(2);
if (offline.length > 1 || (offline.length === 1 && offline[0] !== '--offline')) {
  throw new Error('Only --offline is accepted; backend qualification always runs clean verify.');
}
if (!process.env.JAVA_HOME) {
  throw new Error('Set JAVA_HOME for this command to the portable Temurin 21.0.13+9 JDK.');
}
const release = readFileSync(join(process.env.JAVA_HOME, 'release'), 'utf8');
if (!/^JAVA_RUNTIME_VERSION="21\.0\.13\+9.*"$/m.test(release)
    || !/^IMPLEMENTOR="Eclipse Adoptium"$/m.test(release)) {
  throw new Error('The backend requires the approved Temurin 21.0.13+9 target.');
}
const environment = { ...process.env };
for (const key of ['MVNW_USERNAME', 'MVNW_PASSWORD', 'MVNW_REPOURL', 'MVNW_VERBOSE', 'MAVEN_ARGS',
  'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS']) {
  delete environment[key];
}
environment.MAVEN_SKIP_RC = 'true';
environment.MAVEN_USER_HOME = join(root, 'tmp', 'l02', 'wrapper-home');
environment.MAVEN_OPTS = '-Xmx512m -Djava.awt.headless=true';
mkdirSync(environment.MAVEN_USER_HOME, { recursive: true });
const windows = process.platform === 'win32';
const argumentsForMaven = [
  '--batch-mode', '--no-transfer-progress', '--settings', '.mvn/settings.xml',
  '--global-settings', '.mvn/settings.xml', ...offline, 'clean', 'verify',
];
const command = windows ? 'cmd.exe' : './mvnw';
const argumentsForProcess = windows
  ? ['/d', '/s', '/c', `mvnw.cmd ${argumentsForMaven.join(' ')}`]
  : argumentsForMaven;
const result = spawnSync(command, argumentsForProcess, {
  cwd: join(root, 'backend'), env: environment, stdio: 'inherit', windowsHide: true,
});
if (result.error) {
  throw new Error('Backend wrapper could not start.', { cause: result.error });
}
process.exitCode = result.status ?? 1;
