package org.esupportail.catappsrvs.dao;

import fj.F;
import fj.P2;
import fj.data.Either;
import fj.data.List;
import fj.data.Option;
import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.esupportail.catappsrvs.model.Application;
import org.esupportail.catappsrvs.model.Domaine;
import org.esupportail.catappsrvs.model.utils.Equals;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import static fj.Bottom.error;
import static fj.data.List.list;
import static fj.data.List.single;
import static fj.data.Option.fromNull;
import static fj.data.Option.some;
import static org.dbunit.dataset.datatype.DataType.BIGINT;
import static org.dbunit.dataset.datatype.DataType.INTEGER;
import static org.dbunit.dataset.datatype.DataType.VARCHAR;
import static org.esupportail.catappsrvs.model.Application.Accessibilite.*;
import static org.esupportail.catappsrvs.model.Application.*;
import static org.esupportail.catappsrvs.model.CommonTypes.Code.*;
import static org.esupportail.catappsrvs.model.CommonTypes.Description.*;
import static org.esupportail.catappsrvs.model.CommonTypes.LdapGroup.*;
import static org.esupportail.catappsrvs.model.CommonTypes.Libelle.*;
import static org.esupportail.catappsrvs.model.CommonTypes.Titre.*;
import static org.esupportail.catappsrvs.model.Domaine.*;
import static org.esupportail.catappsrvs.model.Versionned.Version.*;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("SpringJavaAutowiringInspection")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConf.class)
@ActiveProfiles("JDBC")
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class TestDaos {

    @Inject
    DataSource dataSource;

    @Inject
    IDomaineDao domaineDao;

    IDataSet dataSet;

    IDatabaseConnection dbunitConn;

    private final Application firstApp =
            application(
                    version(1),
                    code("APP1"),
                    titre("Application 1"),
                    libelle("L'appli 1"),
                    description(""),
                    buildUrl("http://toto.fr"),
                    Accessible,
                    ldapGroup("UR1:57SI"),
                    list(this.firstDom));

    private final Domaine firstDom =
            domaine(version(1),
                    code("DOM1"),
                    libelle("Domaine 1"),
                    Option.<Domaine>some(null),
                    List.<Domaine>nil(),
                    list(firstApp));

    @Before
    public void setUp() throws DatabaseUnitException, SQLException {
        dataSet = new DefaultDataSet(new DefaultTable[] {
                new DefaultTable("Domaine", new Column[]{
                        new Column("pk", BIGINT),
                        new Column("version", INTEGER),
                        new Column("code", VARCHAR),
                        new Column("libelle", VARCHAR),
                        new Column("parent_pk", BIGINT)
                }){{
                    addRow(domaineToRow(1L, firstDom));
                }},
                new DefaultTable("Application", new Column[]{
                        new Column("pk", BIGINT),
                        new Column("version", INTEGER),
                        new Column("code", VARCHAR),
                        new Column("titre", VARCHAR),
                        new Column("libelle", VARCHAR),
                        new Column("description", VARCHAR),
                        new Column("url", VARCHAR),
                        new Column("accessibilite", VARCHAR),
                        new Column("groupe", VARCHAR),
                }){{
                    addRow(applicationToRow(1L, firstApp));
                }},
                new DefaultTable("DOMAINE_APPLICATION", new Column[]{
                        new Column("domaine_pk", BIGINT),
                        new Column("application_pk", BIGINT)
                }){{
                    addRow(new Object[] {1L, 1L});
                }}
        });
        dbunitConn = new DatabaseConnection(dataSource.getConnection());
        DatabaseOperation.CLEAN_INSERT.execute(dbunitConn, dataSet);
    }

    @After
    public void cleanUp() throws SQLException {
        dbunitConn.close();
    }

    @Test @Transactional
    public void testCreate() {
        final Either<Exception, Domaine> result = domaineDao.create(
                domaine(version(-1),
                        code("DOM2"),
                        libelle("Domaine 2"),
                        some(firstDom),
                        List.<Domaine>nil(),
                        list(firstApp)));

        for (Exception e: result.left()) {
            e.printStackTrace();
            assertTrue("create ne devrait pas lever d'exception", false);
        }

        for (Domaine domaine : result.right()) {
            assertTrue(
                    "create doit se traduire par l'affectation d'une clé primaire",
                    fromNull(domaine.pk()).isSome());
            assertTrue(
                    "create doit se traduire par l'affectation d'une version égale à 1",
                    domaine.version().equals(version(1)));
        }
    }

    @Test
    public void testRead() {
        final Either<Exception, Domaine> result = domaineDao.read(firstDom.code(), some(firstDom.version()));

        for (Exception e: result.left()) {
            e.printStackTrace();
            assertTrue(e instanceof NoSuchElementException
                    ? "read appliquée à des valeurs correctes doit retourner une entité"
                    : "read ne devrait pas lever d'exception", false);
        }

        for (Domaine domaine : result.right())
            assertTrue("read doit retourner la bonne entité", Equals.domaineCompleteEq.eq(firstDom, domaine));
    }

    @Test @Transactional
    public void testUpdate() {
        final Domaine domToCreate = domaine(version(-1),
                code("DOM2"),
                libelle("Domaine 2"),
                Option.<Domaine>none(),
                List.<Domaine>nil(),
                list(firstApp));

        final Either<Exception, Domaine> createdDom = domaineDao.create(domToCreate);

        final Either<Exception, Domaine> dom =
                domaineDao.update(firstDom
                        .withLibelle(libelle("UPDATED Domaine 1"))
                        .withSousDomaines(single(domToCreate)));

        final Either<Exception, Domaine> updatedDom = domaineDao.read(firstDom.code(), Option.<Version>none());

        for (Exception e : dom.left().toList().append(updatedDom.left().toList()))
            throw new AssertionError(e);

        for (P2<Domaine, Domaine> pair : dom.right().toList().zip(updatedDom.right().toList())) {
            assertTrue("update doit mettre à jour les données", Equals.domaineCompleteEq.eq(pair._1(), pair._2()));
            assertTrue("update doit se traduire par une incrémentation de la version",
                    pair._1().version().equals(version(2)) && pair._2().version().equals(version(2)));
        }
    }

    @Test @Transactional
    public void testDelete() {
        assertTrue("delete ne doit pas lever d'exception", domaineDao.delete(firstDom.code()).isRight());
    }

    private Object[] domaineToRow(Long pk, Domaine domaine) {
        return new Object[] {
                pk,
                domaine.version().value(),
                domaine.code().value(),
                domaine.libelle().value(),
                getParentPk(domaine)
        };
    }

    private Object[] applicationToRow(Long pk, Application application) {
        return new Object[] {
                pk,
                application.version().value(),
                application.code().value(),
                application.titre().value(),
                application.libelle().value(),
                application.description().value(),
                application.url(),
                application.accessibilite().toString(),
                application.groupe().value()
        };
    }

    private Long getParentPk(Domaine domaine) {
        return domaine
                .parent()
                .map(new F<Domaine, Long>() {
                    public Long f(Domaine parent) {
                        return parent.pk();
                    }
                })
                .toNull();
    }

    private URL buildUrl(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw error(e.getMessage());
        }
    }

}
