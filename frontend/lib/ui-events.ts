/** 출처 표시 설정이 바뀌어 출처 UI와 홈 목록을 함께 갱신할 때 사용한다. */
export const SOURCE_VISIBILITY_CHANGED_EVENT = "pickdeal:source-visibility-changed";

export interface SourceVisibilityChangedDetail {
  sourceId: number;
  visible: boolean;
}
