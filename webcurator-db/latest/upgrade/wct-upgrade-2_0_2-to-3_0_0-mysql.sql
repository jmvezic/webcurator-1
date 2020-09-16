ALTER TABLE DB_WCT.HARVEST_RESULT MODIFY HR_INDEX INTEGER DEFAULT 0;
ALTER TABLE DB_WCT.ID_GENERATOR MODIFY IG_VALUE BIGINT;

UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(ROL_OID) FROM DB_WCT.WCTROLE), 0)) WHERE IG_TYPE='Role';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(AGC_OID) FROM DB_WCT.AGENCY), 0)) WHERE IG_TYPE='Agency';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(USR_OID) FROM DB_WCT.WCTUSER), 0)) WHERE IG_TYPE='User';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(DC_OID) FROM DB_WCT.DUBLIN_CORE), 0)) WHERE IG_TYPE='DublinCore';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(PO_OID) FROM DB_WCT.PROFILE_OVERRIDES), 0)) WHERE IG_TYPE='PROFILE_OVERRIDE';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(RR_OID) FROM DB_WCT.REJECTION_REASON), 0)) WHERE IG_TYPE='RejectionReason';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(LOGDUR_OID) FROM DB_WCT.WCT_LOGON_DURATION), 0)) WHERE IG_TYPE='LogonDuration';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(BR_OID) FROM DB_WCT.BANDWIDTH_RESTRICTIONS), 0)) WHERE IG_TYPE='Bandwidth';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(PRV_OID) FROM DB_WCT.ROLE_PRIVILEGE), 0)) WHERE IG_TYPE='RolePriv';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(AN_OID) FROM DB_WCT.ANNOTATIONS), 0)) WHERE IG_TYPE='Annotation';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(TSK_OID) FROM DB_WCT.TASK), 0)) WHERE IG_TYPE='Task';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(AHF_OID) FROM DB_WCT.ARC_HARVEST_FILE), 0)) WHERE IG_TYPE='ArcHarvestFile';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(HRC_OID) FROM DB_WCT.HARVEST_RESOURCE), 0)) WHERE IG_TYPE='HarvestResource';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(AUD_OID) FROM DB_WCT.WCTAUDIT), 0)) WHERE IG_TYPE='Audit';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(NOT_OID) FROM DB_WCT.NOTIFICATION), 0)) WHERE IG_TYPE='Notification';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(HR_OID) FROM DB_WCT.HARVEST_RESULT), 0)) WHERE IG_TYPE='HarvestResult';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(PRT_OID) FROM DB_WCT.PERMISSION_TEMPLATE), 0)) WHERE IG_TYPE='PermissionTemplate';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(PC_OID) FROM DB_WCT.PROFILE_CREDENTIALS), 0)) WHERE IG_TYPE='Profile Security Credential';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=(COALESCE((SELECT MAX(PEX_OID) FROM DB_WCT.PERMISSION_EXCLUSION), 0)) WHERE IG_TYPE='PermExclusion';
UPDATE DB_WCT.ID_GENERATOR SET IG_VALUE=GREATEST(
	(COALESCE((SELECT MAX(TI_OID) FROM DB_WCT.TARGET_INSTANCE),0)),
	(COALESCE((SELECT MAX(IC_OID) FROM DB_WCT.INDICATOR_CRITERIA),0)),
	(COALESCE((SELECT MAX(HM_OID) FROM DB_WCT.HEATMAP_CONFIG),0)),
	(COALESCE((SELECT MAX(S_OID) FROM DB_WCT.SCHEDULE),0)),
	(COALESCE((SELECT MAX(AA_OID) FROM DB_WCT.AUTHORISING_AGENT),0)),
	(COALESCE((SELECT MAX(UP_OID) FROM DB_WCT.URL_PATTERN),0)),
	(COALESCE((SELECT MAX(AT_OID) FROM DB_WCT.ABSTRACT_TARGET),0)),
	(COALESCE((SELECT MAX(I_OID) FROM DB_WCT.INDICATOR),0)),
	(COALESCE((SELECT MAX(AT_OID) FROM DB_WCT.GROUP_MEMBER),0)),
	(COALESCE((SELECT MAX(IRL_OID) FROM DB_WCT.INDICATOR_REPORT_LINE),0)),
	(COALESCE((SELECT MAX(S_OID) FROM DB_WCT.SEED),0)),
	(COALESCE((SELECT MAX(UPM_OID) FROM DB_WCT.URL_PERMISSION_MAPPING_VIEW),0)),
	(COALESCE((SELECT MAX(F_OID) FROM DB_WCT.FLAG),0)),
	(COALESCE((SELECT MAX(P_OID) FROM DB_WCT.PROFILE),0)),
	(COALESCE((SELECT MAX(UPM_OID) FROM DB_WCT.URL_PERMISSION_MAPPING),0)),
	(COALESCE((SELECT MAX(SH_OID) FROM DB_WCT.SEED_HISTORY),0)),
	(COALESCE((SELECT MAX(ST_OID) FROM DB_WCT.SITE),0)),
	(COALESCE((SELECT MAX(AT_OID) FROM DB_WCT.ABSTRACT_TARGET_GROUPTYPE_VIEW),0)),
	(COALESCE((SELECT MAX(PE_OID) FROM DB_WCT.PERMISSION),0))
) WHERE IG_TYPE='General';

