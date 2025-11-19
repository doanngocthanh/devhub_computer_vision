package com.devhub.ocr.app.systems.auth;

import java.time.Instant;
import java.util.*;

/**
 * Simple DTO that represents the currently authenticated user and related metadata.
 * Populated from DB rows (Map<String,Object>) or from JWT/claims.
 */
public class UserObject {
	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private Set<String> roles = new HashSet<>();
	private Instant createdAt;
	// optional fields
	private String displayName;

	public UserObject() {
	}

	public UserObject(Long id, String email) {
		this.id = id;
		this.email = email;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles == null ? new HashSet<>() : new HashSet<>(roles);
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getDisplayName() {
		if (displayName != null && !displayName.isBlank()) return displayName;
		if (firstName != null && lastName != null) return firstName + " " + lastName;
		if (firstName != null) return firstName;
		return email;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		return "UserObject{" +
				"id=" + id +
				", email='" + email + '\'' +
				", roles=" + roles +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		UserObject that = (UserObject) o;
		return Objects.equals(id, that.id) && Objects.equals(email, that.email);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, email);
	}

	/**
	 * Build a UserObject from a DB row map (as returned by DatabasePlugin.query).
	 * Accepts common keys: id, email, first_name, last_name, created_at, roles (comma-separated or single).
	 */
	public static UserObject fromMap(Map<String, Object> row) {
		if (row == null) return null;
		UserObject u = new UserObject();
		Object idObj = row.get("id");
		if (idObj != null) {
			try {
				long v = Long.parseLong(String.valueOf(idObj));
				u.setId(v);
			} catch (Exception ignored) {}
		}
		Object e = row.get("email");
		if (e != null) u.setEmail(String.valueOf(e));
		Object fn = row.get("first_name");
		if (fn != null) u.setFirstName(String.valueOf(fn));
		Object ln = row.get("last_name");
		if (ln != null) u.setLastName(String.valueOf(ln));
		Object ca = row.get("created_at");
		if (ca != null) {
			try {
				// accept ISO-8601 or epoch millis
				String s = String.valueOf(ca);
				if (s.matches("\\d+")) {
					long epoch = Long.parseLong(s);
					u.setCreatedAt(Instant.ofEpochMilli(epoch));
				} else {
					u.setCreatedAt(Instant.parse(s));
				}
			} catch (Exception ignored) {}
		}
		// roles may be provided as a collection or comma-separated string
		Object rolesObj = row.get("roles");
		if (rolesObj != null) {
			if (rolesObj instanceof Collection) {
				Set<String> set = new HashSet<>();
				for (Object o : (Collection<?>) rolesObj) set.add(String.valueOf(o));
				u.setRoles(set);
			} else {
				String s = String.valueOf(rolesObj);
				String[] parts = s.split(",");
				Set<String> set = new HashSet<>();
				for (String p : parts) {
					String t = p == null ? "" : p.trim();
					if (!t.isEmpty()) set.add(t);
				}
				u.setRoles(set);
			}
		}
		return u;
	}
}
