-- 부하 측정용 합성 Deal 데이터. 측정 전용 DB(pickdeal_perf)에서만 실행된다.
-- 사용: psql -v n=15000 -f seed-deals.sql   (기존 Deal·그룹·출처를 모두 지우고 n건을 새로 만든다)
-- 분포 근거(2026-10-24 운영 지표): 출처별 신규 비율 퀘이사존 88 : 개드립 77 : 쿨엔조이 67, 게시 시각 90일 보관 범위.
\set ON_ERROR_STOP on

DO $$
BEGIN
    IF current_database() <> 'pickdeal_perf' THEN
        RAISE EXCEPTION 'seed-deals.sql은 pickdeal_perf DB에서만 실행한다 (현재: %)', current_database();
    END IF;
END $$;

TRUNCATE deal_group, deal, source_visibility, keyword, source RESTART IDENTITY CASCADE;

INSERT INTO source (name, base_url, code) VALUES
    ('퀘이사존', 'https://quasarzone.com', 'quasarzone'),
    ('개드립', 'https://www.dogdrip.net', 'dogdrip'),
    ('쿨엔조이', 'https://coolenjoy.net', 'coolenjoy');

-- 같은 n이면 같은 데이터가 나오도록 난수를 고정한다.
SELECT setseed(0.4242);

WITH params AS (
    SELECT
        ARRAY['쿠팡','네이버','G마켓','11번가','옥션','SSG','롯데온','알리익스프레스','아마존','지마켓',
              '위메프','티몬','컬리','오늘의집','무신사','다나와','인터파크','카카오톡선물하기','배민','기타'] AS shops,
        ARRAY['삼성','LG','로지텍','레노버','애플','샌디스크','WD','크루셜','ASUS','MSI',
              '농심','오뚜기','CJ','동원','다이슨','필립스','나이키','아디다스','닌텐도','소니'] AS brands,
        ARRAY['SSD 1TB','무선 마우스','기계식 키보드','27인치 모니터','라면 40봉','참치 20캔','에어프라이어',
              '블루투스 이어폰','게이밍 노트북','외장하드 2TB','운동화','후드티','컨트롤러','그래픽카드',
              '공기청정기','전동칫솔','상품권 5만원','포인트 적립','생수 2L 24병','캡슐커피 100개'] AS products,
        ARRAY['PC/하드웨어','노트북/모바일','가전/TV','식품','생활용품','의류/잡화','게임/SW','상품권/쿠폰','기타'] AS q_cats,
        ARRAY['PC관련','가전','모바일','게임','식품','의류잡화','화장품','쿠폰','이벤트','기타'] AS c_cats
),
rows AS (
    SELECT
        i,
        random() AS r_source,
        random() AS r_shop,
        random() AS r_brand,
        random() AS r_product,
        random() AS r_cat,
        random() AS r_price,
        random() AS r_status,
        random() AS r_posted,
        random() AS r_thumb,
        random() AS r_desc,
        random() AS r_desc_len,
        random() AS r_comment,
        random() AS r_product_url
    FROM generate_series(1, :n) AS i
)
INSERT INTO deal (source_id, external_id, title, description, price, currency, category, shop_name,
                  comment_count, thumbnail_url, original_url, product_url, status, posted_at, collected_at)
SELECT
    src.id,
    (1000000 + r.i)::text,
    '[' || shop || '] ' || brand || ' ' || product || ' ' || (r.i % 97 + 1) || '차 (' ||
        CASE WHEN r.r_price < 0.1 THEN '가격 정보 없음' ELSE to_char(price, 'FM999,999,999') || '원/무료' END || ')',
    CASE WHEN r.r_desc < 0.3
         THEN repeat(brand || ' ' || product || ' 할인 정보입니다. 배송비와 카드 혜택을 확인하세요. ', 2 + floor(r.r_desc_len * 8)::int)
    END,
    CASE WHEN r.r_price < 0.1 THEN NULL ELSE price END,
    'KRW',
    CASE WHEN src.code = 'coolenjoy' THEN p.c_cats[1 + floor(r.r_cat * array_length(p.c_cats, 1))::int]
         ELSE p.q_cats[1 + floor(r.r_cat * array_length(p.q_cats, 1))::int] END,
    shop,
    CASE WHEN src.code = 'dogdrip' AND r.r_comment < 0.65 THEN NULL ELSE floor(r.r_comment * 200)::int END,
    CASE WHEN src.code <> 'coolenjoy' AND r.r_thumb < 0.95 THEN 'https://img.example.com/' || r.i || '.jpg' END,
    src.base_url || '/deal/' || r.i,
    CASE WHEN r.r_product_url < 0.05 THEN 'https://shop.example.com/item/' || r.i END,
    CASE WHEN r.r_status < 0.85 THEN 'ACTIVE' WHEN r.r_status < 0.95 THEN 'EXPIRED' ELSE 'SOLD_OUT' END,
    now() - (r.r_posted * interval '90 days'),
    now() - (r.r_posted * interval '90 days') + interval '5 minutes'
FROM rows r
CROSS JOIN params p
CROSS JOIN LATERAL (
    SELECT p.shops[1 + floor(power(r.r_shop, 2) * array_length(p.shops, 1))::int] AS shop,  -- 앞쪽 쇼핑몰에 몰리게
           p.brands[1 + floor(r.r_brand * array_length(p.brands, 1))::int] AS brand,
           p.products[1 + floor(r.r_product * array_length(p.products, 1))::int] AS product,
           round((1000 + power(r.r_price, 3) * 1999000)::numeric, -2) AS price
) v
JOIN source src ON src.code = CASE WHEN r.r_source < 0.38 THEN 'quasarzone'
                                    WHEN r.r_source < 0.71 THEN 'dogdrip'
                                    ELSE 'coolenjoy' END;

-- 교차 출처 그룹: 퀘이사존·개드립 딜을 순서대로 짝지어 약 5%를 2건짜리 그룹으로 묶는다.
CREATE TEMP TABLE perf_pairs AS
SELECT a.id AS a_id, b.id AS b_id
FROM (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM deal WHERE source_id = 1 AND id % 13 = 0) a
JOIN (SELECT id, row_number() OVER (ORDER BY id) AS rn FROM deal WHERE source_id = 2 AND id % 13 = 1) b USING (rn);

INSERT INTO deal_group (representative_deal_id, canonical_title)
SELECT p.a_id, d.title FROM perf_pairs p JOIN deal d ON d.id = p.a_id;

UPDATE deal d SET group_id = g.id
FROM deal_group g JOIN perf_pairs p ON p.a_id = g.representative_deal_id
WHERE d.id IN (p.a_id, p.b_id);

ANALYZE;

SELECT count(*) AS deals,
       count(DISTINCT group_id) AS groups,
       count(*) FILTER (WHERE group_id IS NOT NULL) AS grouped_deals
FROM deal;
