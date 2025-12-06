-- Create table
create table ISIMPLE_OV_UNIS_CUS
(
  icusnum    NUMBER(12) not null,
  cclientid  VARCHAR2(64) not null
);

comment on column ISIMPLE_OV_UNIS_CUS.icusnum
  is 'Клиент в АБС';
comment on column ISIMPLE_OV_UNIS_CUS.cclientid
  is 'Клиент в UNISTREAM';

alter table ISIMPLE_OV_UNIS_CUS
  add constraint PK_ISIMPLE_OV_UNI_CUS primary key (ICUSNUM)
  using INDEX;
