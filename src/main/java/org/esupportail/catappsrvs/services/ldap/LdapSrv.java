package org.esupportail.catappsrvs.services.ldap;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import fj.data.Either;
import fj.data.List;
import lombok.Value;
import org.esupportail.catappsrvs.model.User;

import static fj.data.Either.right;
import static fj.data.List.list;
import static org.esupportail.catappsrvs.model.CommonTypes.LdapGroup;
import static org.esupportail.catappsrvs.model.CommonTypes.LdapGroup.*;

@Value(staticConstructor = "ldapSrv")
public class LdapSrv implements ILdap {
    String baseDn;
    LDAPInterface ldap;

    @Override
    public Either<Exception, List<LdapGroup>> getGroups(final User user) {
        final Filter uidFilter = Filter.createEqualityFilter("uid", user.uid.value);
        return right(list(ldapGroup("UR1")));
//        try {
//            final SearchRequest request =
//                    new SearchRequest(baseDn, SearchScope.ONE, uidFilter, "memberOf");
//            final java.util.List<SearchResultEntry> results =
//                    ldap.search(request).getSearchEntries();
//            return iif(!results.isEmpty(),
//                    new P1<List<LdapGroup>>() {
//                        public List<LdapGroup> _1() {
//                            return iterableList(results.get(0).getAttributes())
//                                    .map(new F<Attribute, LdapGroup>() {
//                                        public LdapGroup f(Attribute attribute) {
//                                            return ldapGroup(attribute.getValue());
//                                        }
//                                    });
//                        }
//                    },
//                    new P1<Exception>() {
//                        public Exception _1() {
//                            return new Exception("Aucune entrée dans le ldap ne correspond à " + user);
//                        }
//                    }
//            );
//        } catch (LDAPException e) {
//            return left((Exception) e);
//        }
    }
}
