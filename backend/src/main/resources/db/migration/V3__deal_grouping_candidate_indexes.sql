-- 교차 출처 그룹 후보 조회(DealGroupingService)의 상품 URL·가격 일치 조건이 전체 스캔하지 않도록 한다.
CREATE INDEX idx_deal_product_url ON deal (product_url);
CREATE INDEX idx_deal_price ON deal (price);
