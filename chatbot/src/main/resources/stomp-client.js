#!/usr/bin/env node
/*
 Simple Node STOMP test client for command line (no HTML).
 Usage:
   1) Install dependencies in the chatbot folder:
      npm install @stomp/stompjs ws
   2) Run:
      node stomp-client.js

 This edited version prompts for:
  - WebSocket/STOMP endpoint (e.g. ws://localhost:8080/chat/websocket)
  - Broker destination(s) to subscribe to (comma-separated, e.g. /topic/messages)
  - Username to use as sender
*/

const { Client } = require('C:/Program Files/nodejs/node_modules/@stomp/stompjs');
const WebSocket = require('C:/Program Files/nodejs/node_modules/ws'); // ws still needs local/global install
const readline = require('readline');

// ANSI colors
const COL_RESET = '\x1b[0m';
const COL_BLUE = '\x1b[34m';
const COL_YELLOW = '\x1b[33m';
const COL_GREEN = '\x1b[32m';
const COL_RED = '\x1b[31m';

function blue(text){ console.log(COL_BLUE + text + COL_RESET); }
function yellow(text){ console.log(COL_YELLOW + text + COL_RESET); }
function green(text){ console.log(COL_GREEN + text + COL_RESET); }
function red(text){console.log(COL_RED + text + COL_RESET); }

// Simple arg parse (allows override if provided)
const argv = process.argv.slice(2);
let urlArg = null;
for (let i=0;i<argv.length;i++){
  const a = argv[i];
  if (a === '--url' && argv[i+1]) { urlArg = argv[i+1]; i++; }
  else if (a.startsWith('--url=')) { urlArg = a.split('=')[1]; }
}

console.log('Interactive STOMP test client');

const rl = readline.createInterface({ input: process.stdin, output: process.stdout, terminal: true });

function question(prompt) {
  return new Promise(resolve => rl.question(prompt, answer => resolve(answer.trim())));
}

(async function main(){
  try {
    const defaultUrl = urlArg || 'ws://localhost:8080/chatbot/websocket';
    const endpoint = await question(`WebSocket URL [${defaultUrl}]: `) || defaultUrl;

    // subscribe to private user queue and broadcast by default for MVP
    const defaultSubs = '/queue/msg,/broadcast/msg';
    const subsInput = await question(`Broker destination(s) to subscribe (comma-separated) [${defaultSubs}]: `) || defaultSubs;
    const subscriptions = subsInput.split(',').map(s => s.trim()).filter(Boolean);

    const defaultUser = 'cmd-client';
    const username = await question(`Username to use as sender [${defaultUser}]: `) || defaultUser;

    // prompt for the send destination (where messages will be published)
    // default targets the private message handler on the server
    const defaultSendDest = '/app/message';
    const sendDestination = await question(`Send destination [${defaultSendDest}]: `) || defaultSendDest;

    const defaultToken = 'default-token';
    const token = await question(`Authentication token [${defaultToken}]: `) || defaultToken;

    blue(`Connecting to: ${endpoint}`);

    const client = new Client({
      webSocketFactory: () => new WebSocket(endpoint),
      reconnectDelay: 5000,
      debug: (str) => { blue('[STOMP] ' + str); }
    });

    client.onConnect = (frame) => {
      green('Connected.');
      green('Subscribing to: ' + subscriptions.join(', '));

      subscriptions.forEach(dest => {
        try {
          client.subscribe(dest, (message) => {
            let body = message.body;
            try { body = JSON.parse(message.body); body = JSON.stringify(body); } catch(e) {}
            yellow(`[RECEIVED ${dest}] ` + body);
          });
        } catch (e) {
          red(`Failed to subscribe to ${dest}: ${e}`);
        }
      });

      green('Type message and press Enter to send. Type "exit" to quit.');

      // now listen for message lines from stdin
      rl.on('line', (line) => {
        const trimmed = line.trim();
        if (!trimmed) return;
        if (trimmed.toLowerCase() === 'exit') {
          blue('Disconnecting...');
          try { client.deactivate(); } catch(e) { }
          rl.close();
          return;
        }
        const payload = { sender: username, token:token, content: trimmed };
        try {
          if (client.publish) {
            client.publish({ destination: sendDestination, body: JSON.stringify(payload) });
          } else if (client.send) {
            client.send(sendDestination, {}, JSON.stringify(payload));
          } else {
            blue('No publish/send method available on STOMP client');
          }
          blue('[SENT] ' + JSON.stringify(payload));
        } catch (e) {
          blue('[ERROR] send failed: ' + e);
        }
      });
    };

    client.onStompError = (frame) => {
      red('[STOMP ERROR] ' + (frame && (frame.message || frame['message'])) );
    };

    client.onWebSocketClose = (evt) => {
      red('[WS] closed');
    };

    client.onWebSocketError = (evt) => {
      red('[WS] error: ' + evt);
    };

    client.activate();

    process.on('SIGINT', () => {
      blue('\nCaught interrupt. Disconnecting...');
      try { client.deactivate(); } catch(e) {}
      process.exit(0);
    });

  } catch (err) {
    red('Fatal error: ' + err);
    rl.close();
    process.exit(1);
  }
})();
