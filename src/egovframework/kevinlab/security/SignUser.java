package egovframework.kevinlab.security;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;

public class SignUser implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String username;
    private String password;
    private String email;
    private String userautn;
    private String firstName;
    private String lastName;
    private String sSiteId;

    /* Spring Security related fields*/
    private List<SignRole> 	authorities;
    private boolean 		accountNonExpired 		= true;
    private boolean 		accountNonLocked 		= true;
    private boolean 		credentialsNonExpired 	= true;
    private boolean 		enabled 				= true;


	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUserautn() {
		return userautn;
	}
	public void setUserautn(String userautn) {
		this.userautn = userautn;
	}
	public String getFirstName() {
		return firstName;
	}
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}
	public String getLastName() {
		return lastName;
	}
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	public List<SignRole> getAuthorities() {
		return authorities;
	}
	public void setAuthorities(List<SignRole> authorities) {
		this.authorities = authorities;
	}
	public boolean isAccountNonExpired() {
		return accountNonExpired;
	}
	public void setAccountNonExpired(boolean accountNonExpired) {
		this.accountNonExpired = accountNonExpired;
	}
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}
	public void setAccountNonLocked(boolean accountNonLocked) {
		this.accountNonLocked = accountNonLocked;
	}
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired;
	}
	public void setCredentialsNonExpired(boolean credentialsNonExpired) {
		this.credentialsNonExpired = credentialsNonExpired;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
    public String getsSiteId() {
        return sSiteId;
    }
    public void setsSiteId(String sSiteId) {
        this.sSiteId = sSiteId;
    }

 }