package egovframework.kevinlab.dto;

public class UserDto {

	private int seq;

	private String sUserId;
    private String sPwd;
    private String sSiteId;
    private String sEmpId;
    private String sEmpNm;
    private String sDeptId;
    private String sTelNo;
    private String sAuth;

	private String auth;
	private String role;


    public int getSeq() {
        return seq;
    }
    public void setSeq(int seq) {
        this.seq = seq;
    }
    public String getsUserId() {
        return sUserId;
    }
    public void setsUserId(String sUserId) {
        this.sUserId = sUserId;
    }
    public String getsPwd() {
        return sPwd;
    }
    public void setsPwd(String sPwd) {
        this.sPwd = sPwd;
    }
    public String getsSiteId() {
        return sSiteId;
    }
    public void setsSiteId(String sSiteId) {
        this.sSiteId = sSiteId;
    }
    public String getsEmpId() {
        return sEmpId;
    }
    public void setsEmpId(String sEmpId) {
        this.sEmpId = sEmpId;
    }
    public String getsEmpNm() {
        return sEmpNm;
    }
    public void setsEmpNm(String sEmpNm) {
        this.sEmpNm = sEmpNm;
    }
    public String getsDeptId() {
        return sDeptId;
    }
    public void setsDeptId(String sDeptId) {
        this.sDeptId = sDeptId;
    }
    public String getsTelNo() {
        return sTelNo;
    }
    public void setsTelNo(String sTelNo) {
        this.sTelNo = sTelNo;
    }
    public String getsAuth() {
        return sAuth;
    }
    public void setsAuth(String sAuth) {
        this.sAuth = sAuth;
    }
    public String getAuth() {
        return auth;
    }
    public void setAuth(String auth) {
        this.auth = auth;
    }
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }



}
