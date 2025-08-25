// notifier.js
// ---------------------------------------------
// INSTALL ONCE:
//   npm init -y
//   npm i axios node-cron imapflow
// RUN:
//   node notifier.js
// NOTE: Make sure you have an audio player available:
//   macOS: built-in 'afplay' works out of the box
//   Linux: install one of: mpg123, mplayer, or ffmpeg (ffplay)
//   Windows: uses PowerShell SoundPlayer
// ---------------------------------------------

const axios = require("axios");
const cron = require("node-cron");
const { ImapFlow } = require("imapflow");
const { spawn } = require("child_process");
const path = require("path");

// =============================================
// CONFIG — easy to edit (no .env required)
// =============================================
const CONFIG = {
  // ---- Function 1: HTTP piscine page check ----
  HTTP: {
    url: "https://apply.42abudhabi.ae/users/1225298/id_checks_users",
    method: "POST", // curl had --data '' so it's a POST
    body: "", // empty body, same as your curl
    noSlotsText: "There are no available piscines right now", // exact string to look for
    headers: {
      accept:
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
      "accept-encoding": "gzip, deflate, br, zstd",
      "accept-language": "en-GB,en;q=0.9,en-US;q=0.8,fr;q=0.7,ml;q=0.6",
      "cache-control": "no-cache",
      cookie: `_scid=noOSqzHmjYqvpfWkVsGDR4qYXiqs703s; _fbp=fb.1.1749910519931.318927756812476318; _tt_enable_cookie=1; _ttp=01JXQCQ107NWE8M9S3X0QG1456_.tt.1; cookieconsent_status=allow; locale=en; _gid=GA1.2.1924434881.1755887521; _ScCbts=%5B%22607%3Bchrome.2%3A2%3A5%22%2C%22626%3Bchrome.2%3A2%3A5%22%5D; _sctr=1%7C1755806400000; _gcl_au=1.1.965288889.1749910519.861386868.1755887538.1755887539; _admissions_session_production=e69da6aad9b7eb0adeb246c6aa1a2faa; _ga=GA1.2.1596495760.1749910520; _scid_r=owOSqzHmjYqvpfWkVsGDR4qYXiqs703sEbmQ3Q; ttcsid=1755902309516::5jThrHy7o2S7BeEvJ26b.22.1755902375566; ttcsid_CPHGJRJC77UAVM1484PG=1755902309516::RTahyxS85xzkhlx1FJyL.22.1755902375786; ttcsid_CQB3KEBC77UCUPKFUIH0=1755902309517::pWFtIjyd1lg4JWq81J_Z.22.1755902375786; ttcsid_BTG7E331811BQC941EDG=1755902309516::7blj5eBmtoM-pV7QMEWy.22.1755902375786; ph_phc_w0Uj0THoEoBYOEhEmdFtz36tIi21gTdD7eINnBpF3Dc_posthog=%7B%22distinct_id%22%3A%2201976ecb-8511-7a9a-83c8-013651896d52%22%2C%22%24sesid%22%3A%5B1755902376647%2C%220198d3ef-fac4-746a-9075-c06849468644%22%2C1755902376644%5D%2C%22%24initial_person_info%22%3A%7B%22r%22%3A%22https%3A%2F%2Fwww.google.com%2F%22%2C%22u%22%3A%22https%3A%2F%2F42abudhabi.ae%2F%22%7D%7D; _ga_8M0TZSR8V1=GS2.1.s1755902308$o27$g1$t1755902377$j58$l0$h1089439285; _ga_6H0SY0TE1H=GS2.1.s1755902309$o27$g1$t1755902377$j58$l0$h0; _admissions_session_production=e69da6aad9b7eb0adeb246c6aa1a2faa; _mkra_stck=8b5bad696a8c3cbbc58c005a89e3e8a0%3A1756057847.2550917; locale=en`,
      "sec-ch-ua-platform": "macOS",
      "sec-fetch-dest": "document",
      "sec-fetch-mode": "navigate",
      "sec-fetch-site": "none",
      "sec-fetch-user": "?1",
      "upgrade-insecure-requests": "1",
      "user-agent":
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit(537.36) (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36",
    },
  },

  // ---- Function 2: Email check (toggle + settings) ----
  EMAIL: {
    enabled: true, // turn off by setting to false
    host: "imap.gmail.com", // e.g. imap.zoho.com, outlook.office365.com
    port: 993,
    secure: true,
    user: "your-email@example.com",
    pass: "your-app-password-or-imap-password", // use an App Password if 2FA enabled
    mailbox: "INBOX",
    fromAddress: "no-reply@apply.42abudhabi.ae",
  },

  // ---- MP3 to play ----
  AUDIO: {
    file: path.resolve("./iphone.mp3"), // put alert.mp3 next to this file
  },
};

// =============================================
// Reusable MP3 player (cross-platform attempts)
// =============================================
async function playAlert() {
  const mp3 = CONFIG.AUDIO.file;
  const platform = process.platform;

  const attempts = [];

  if (platform === "darwin") {
    attempts.push(["afplay", [mp3]]);
  } else if (platform === "win32") {
    // Use PowerShell SoundPlayer
    const script = `Add-Type -AssemblyName presentationCore; (New-Object System.Media.SoundPlayer "${mp3.replace(
      /\\/g,
      "/"
    )}").PlaySync();`;
    attempts.push(["powershell", ["-NoProfile", "-Command", script]]);
  }

  // Generic fallbacks for Linux/others
  attempts.push(["mpg123", [mp3]]);
  attempts.push(["mplayer", [mp3]]);
  attempts.push(["ffplay", ["-nodisp", "-autoexit", mp3]]);

  // Try each command until one succeeds
  for (const [cmd, args] of attempts) {
    try {
      await new Promise((resolve, reject) => {
        const p = spawn(cmd, args, { stdio: "ignore" });
        p.on("error", reject);
        p.on("exit", (code) =>
          code === 0 ? resolve() : reject(new Error(`${cmd} exited ${code}`))
        );
      });
      console.log(`[${new Date().toISOString()}] Played alert via ${cmd}`);
      return;
    } catch (_) {
      // try next
    }
  }

  console.error(
    "No audio player found. Install mpg123/mplayer/ffmpeg (or use macOS/Windows defaults)."
  );
}

// =============================================
// Function 1: Check piscine HTML page
// =============================================
async function checkPiscine() {
  try {
    const res = await axios.request({
      url: CONFIG.HTTP.url,
      method: CONFIG.HTTP.method,
      data: CONFIG.HTTP.body,
      headers: CONFIG.HTTP.headers,
      responseType: "text",
      decompress: true,
      maxRedirects: 5,
      validateStatus: (s) => s >= 200 && s < 400,
    });

    const html = String(res.data || "");
    const containsNoSlots = html.includes(CONFIG.HTTP.noSlotsText);

    if (!containsNoSlots) {
      console.log(
        `[${new Date().toISOString()}] Piscine: POSSIBLE AVAILABILITY (text not found)`
      );
      await playAlert();
    } else {
      console.log(
        `[${new Date().toISOString()}] Piscine: "${
          CONFIG.HTTP.noSlotsText
        }" found (no availability).`
      );
    }
  } catch (err) {
    console.error(
      `[${new Date().toISOString()}] Piscine check error:`,
      err.message
    );
  }
}

// =============================================
// Function 2: Check unseen emails from sender
// =============================================
async function checkEmail() {
  if (!CONFIG.EMAIL.enabled) {
    console.log(`[${new Date().toISOString()}] Email check disabled.`);
    return;
  }

  let client;
  try {
    client = new ImapFlow({
      host: CONFIG.EMAIL.host,
      port: CONFIG.EMAIL.port,
      secure: CONFIG.EMAIL.secure,
      auth: { user: CONFIG.EMAIL.user, pass: CONFIG.EMAIL.pass },
      logger: false,
    });

    await client.connect();
    await client.mailboxOpen(CONFIG.EMAIL.mailbox);

    // Search unseen FROM specific address
    const unseenFrom = await client.search({
      seen: false,
      from: CONFIG.EMAIL.fromAddress,
    });

    if (unseenFrom && unseenFrom.length > 0) {
      console.log(
        `[${new Date().toISOString()}] Found ${
          unseenFrom.length
        } unseen email(s) from ${CONFIG.EMAIL.fromAddress}`
      );
      await playAlert();
    } else {
      console.log(
        `[${new Date().toISOString()}] No unseen emails from ${
          CONFIG.EMAIL.fromAddress
        }`
      );
    }
  } catch (err) {
    console.error(
      `[${new Date().toISOString()}] Email check error:`,
      err.message
    );
  } finally {
    if (client) {
      try {
        await client.logout();
      } catch (_) {}
    }
  }
}

// =============================================
// Scheduler: run now, then every minute
// =============================================
async function runOnce() {
  await Promise.allSettled([
  checkPiscine(), 
  //checkEmail()
  ]);
}

runOnce(); // run immediately on start
cron.schedule("* * * * *", runOnce); // then every minute
