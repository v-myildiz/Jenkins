/*
 * The MIT License
 *
 * Copyright 2020 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.acegisecurity;

import hudson.security.ACL;
import hudson.security.SecurityRealm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("deprecation")
public class AuthenticationTest {

    @Test
    public void system() {
        assertEquality(ACL.SYSTEM, ACL.SYSTEM2);
    }

    @Test
    public void anonymous() {
        assertEquality(Jenkins.ANONYMOUS, Jenkins.ANONYMOUS2);
    }

    @Test
    public void user() {
        assertEquality(new org.acegisecurity.providers.UsernamePasswordAuthenticationToken("user", "pass", new GrantedAuthority[] {SecurityRealm.AUTHENTICATED_AUTHORITY}),
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user", "pass", Collections.singleton(SecurityRealm.AUTHENTICATED_AUTHORITY2)));
    }

    private void assertEquality(Authentication acegi, org.springframework.security.core.Authentication spring) {
        Authentication acegi2 = Authentication.fromSpring(spring);
        org.springframework.security.core.Authentication spring2 = acegi.toSpring();
        Authentication acegi3 = Authentication.fromSpring(spring2);
        org.springframework.security.core.Authentication spring3 = acegi2.toSpring();
        Collection<Executable> checks = new ArrayList<>();
        Authentication[] acegis = new Authentication[] {acegi, acegi2, acegi3};
        org.springframework.security.core.Authentication[] springs = new org.springframework.security.core.Authentication[] {spring, spring2, spring3};
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int _i = i;
                int _j = j;
                checks.add(() -> assertEquals(acegis[_i], acegis[_j], "Acegi #" + (_i + 1) + " == #" + (_j + 1)));
            }
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int _i = i;
                int _j = j;
                checks.add(() -> assertEquals(springs[_i], springs[_j], "Spring #" + (_i + 1) + " == #" + (_j + 1)));
            }
        }
        assertAll(checks);
    }

}
