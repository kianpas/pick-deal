-- USD 센트 단위를 보존한다. 기존 BIGINT 원화는 값 변경 없이 소수점 2자리 금액으로 확장한다.
ALTER TABLE deal ALTER COLUMN price TYPE NUMERIC(21, 2);
ALTER TABLE deal ALTER COLUMN original_price TYPE NUMERIC(21, 2);
