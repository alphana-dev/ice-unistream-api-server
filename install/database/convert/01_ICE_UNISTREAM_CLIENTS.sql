CREATE OR REPLACE VIEW UNISTREAM_CLIENTS AS
SELECT
    -- PK
    CAST(to_char(CUS.icusnum) AS VARCHAR2(100)) AS CLIENT_ID,
    CAST(v.calfa_3           AS VARCHAR2(3))     AS COUNTRY_OF_RESIDENCE,
    CAST(cus.ccusfirst_name  AS VARCHAR2(100))   AS FIRST_NAME,
    CAST(cus.ccuslast_name   AS VARCHAR2(100))   AS LAST_NAME,
    CAST(cus.ccusmiddle_name AS VARCHAR2(100))   AS MIDDLE_NAME,
    CAST(DECODE(pcusattr.get_cli_atr ( 359, cus.icusnum, sysdate, 0, 0),'G',1,'M',0, 'M', 0, 'F', 1, 0) AS VARCHAR2(10))  AS GENDER,
    CAST(pcusattr.get_cli_atr( 209, cus.icusnum, sysdate, 0, 0) AS VARCHAR2(255))   AS BIRTH_PLACE,
    CAST(CUS.DCUSBIRTHDAY  AS TIMESTAMP)       AS BIRTH_DATE,
    CAST( SUBSTR(regexp_replace(cbsutil_dbo.get_phone( cus.icusnum, 0 ),'[^[[:digit:]]]*'),-11) AS VARCHAR2(50)) AS PHONE_NUMBER,
    CAST(CUS.CCUSNUMNAL AS VARCHAR2(20))    AS T_I_NUMBER,
    CAST(NULL AS VARCHAR2(50))    AS KAZ_ID,
    CAST(util_dm2.get_cus_address(cus.icusnum, 1) AS VARCHAR2(500))   AS ADDRESS_STRING,
    CAST(a.kv_type   || ' ' || a.kv      AS VARCHAR2(50))    AS APARTMENT,
    CAST(a.dom AS VARCHAR2(50))          AS BUILDING,
    CAST(a.city_type || ' ' || a.city    AS VARCHAR2(100))   AS CITY,
    CAST(a.country AS VARCHAR2(3))       AS COUNTRY_CODE,
    CAST(a.dom AS VARCHAR2(50))          AS HOUSE,
    CAST(a.post_index AS VARCHAR2(20))   AS POSTCODE,
    CAST(a.reg_name AS VARCHAR2(100))    AS STATE,
    CAST(a.infr_name AS VARCHAR2(255))   AS STREET,
    CAST(DECODE(d.id_doc_tp, 21, 'RUS', 51, 'ABH', '???') AS VARCHAR2(50)) AS DOCUMENT_TYPE,
    CAST(d.doc_ser AS VARCHAR2(20))      AS DOCUMENT_SERIES,
    CAST(d.doc_num AS VARCHAR2(20))      AS DOCUMENT_NUMBER,
    CAST(d.doc_agency AS VARCHAR2(255))  AS DOCUMENT_ISSUER,
    CAST(d.doc_subdiv AS VARCHAR2(20))   AS D_ISSUER_D_CODE,
    CAST(d.doc_date AS TIMESTAMP)        AS DOCUMENT_ISSUE_DATE,
    CAST(d.doc_active_end AS TIMESTAMP)  AS DOCUMENT_EXPIRY_DATE,
    CAST(d.doc_cnt AS VARCHAR2(10))      AS DOCUMENT_STATE,

    CAST(NULL AS TIMESTAMP)              AS CREATE_DATE
FROM
    CUS, CUS_DOCUM d, V_OK_SM v, cus_addr a
WHERE d.icusnum = cus.icusnum AND d.pref = 'Y'
  AND v.calfa_2 = cus.ccuscountry1
  AND a.icusnum = cus.icusnum
  AND a.addr_type = 0
