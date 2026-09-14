// production 빌드는 안전하게 조회 전용이 기본값이다. UI 설정은 API 접근 제어를 대신하지 않는다.
export const READ_ONLY = process.env.NEXT_PUBLIC_READ_ONLY !== undefined
  ? process.env.NEXT_PUBLIC_READ_ONLY !== "false"
  : process.env.NODE_ENV === "production";
