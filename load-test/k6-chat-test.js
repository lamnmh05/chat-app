import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const USERNAME = __ENV.USERNAME || "demo";
const PASSWORD = __ENV.PASSWORD || "password123";

export const options = {
  vus: 10,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<800"],
  },
};

function jsonParams(token) {
  const headers = {
    "Content-Type": "application/json",
  };

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  return { headers };
}

export default function () {
  const loginPayload = JSON.stringify({
    identifier: USERNAME,
    password: PASSWORD,
  });

  const loginRes = http.post(`${BASE_URL}/api/auth/login`, loginPayload, jsonParams());
  check(loginRes, {
    "login success": (res) => res.status === 200,
  });

  if (loginRes.status !== 200) {
    sleep(1);
    return;
  }

  const token = loginRes.json("accessToken");

  const guildsRes = http.get(`${BASE_URL}/api/guilds/my`, jsonParams(token));
  check(guildsRes, {
    "get guilds success": (res) => res.status === 200,
  });

  const guilds = guildsRes.status === 200 ? guildsRes.json() : [];
  if (!guilds.length) {
    sleep(1);
    return;
  }

  const guildId = guilds[0].id;

  const channelsRes = http.get(`${BASE_URL}/api/guilds/${guildId}/channels`, jsonParams(token));
  check(channelsRes, {
    "get channels success": (res) => res.status === 200,
  });

  const channels = channelsRes.status === 200 ? channelsRes.json() : [];
  if (!channels.length) {
    sleep(1);
    return;
  }

  const channelId = channels[0].id;

  const messagesRes = http.get(`${BASE_URL}/api/channels/${channelId}/messages`, jsonParams(token));
  check(messagesRes, {
    "get messages success": (res) => res.status === 200,
  });

  const sendMessagePayload = JSON.stringify({
    content: `k6 load test ${Date.now()}`,
    type: "TEXT",
    replyToMessageId: null,
    attachmentIds: [],
  });

  const sendMessageRes = http.post(
    `${BASE_URL}/api/channels/${channelId}/messages`,
    sendMessagePayload,
    jsonParams(token)
  );

  check(sendMessageRes, {
    "send message success": (res) => res.status === 200,
  });

  sleep(1);
}
