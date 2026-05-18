package com.library.model;

import java.time.LocalDate;

/**
 * Represents a library member.
 */
public class Member {
    private int memberId;
    private String name;
    private String email;
    private String phone;
    private LocalDate membershipDate;
    private int activeLoanCount;

    public Member() {}

    public Member(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.membershipDate = LocalDate.now();
        this.activeLoanCount = 0;
    }

    // Getters and Setters
    public int getMemberId()                    { return memberId; }
    public void setMemberId(int memberId)       { this.memberId = memberId; }

    public String getName()                     { return name; }
    public void setName(String name)            { this.name = name; }

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }

    public String getPhone()                    { return phone; }
    public void setPhone(String phone)          { this.phone = phone; }

    public LocalDate getMembershipDate()                       { return membershipDate; }
    public void setMembershipDate(LocalDate membershipDate)    { this.membershipDate = membershipDate; }

    public int getActiveLoanCount()                     { return activeLoanCount; }
    public void setActiveLoanCount(int activeLoanCount) { this.activeLoanCount = activeLoanCount; }

    @Override
    public String toString() {
        return String.format("Member{id=%d, name='%s', email='%s', activeLoans=%d}",
                memberId, name, email, activeLoanCount);
    }
}
