/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.  
* <p>
*/ 

package org.olat.core.commons.persistence;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.jpa.HibernateEntityManager;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.hibernate.type.Type;
import org.infinispan.manager.EmbeddedCacheManager;
import org.olat.core.configuration.Destroyable;
import org.olat.core.id.Persistable;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.DBRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;


/**
 * A <b>DB </b> is a central place to get a Entity Managers. It acts as a
 * facade to the database, transactions and Queries. The hibernateSession is
 * lazy loaded per thread.
 * 
 * @author Andreas Ch. Kapp
 * @author Christian Guretzki
 */
public class DBImpl implements DB, Destroyable {
	private static final OLog log = Tracing.createLoggerFor(DBImpl.class);
	private static final int MAX_DB_ACCESS_COUNT = 500;
	private static DBImpl INSTANCE;
	
	private String dbVendor;
	private static EntityManagerFactory emf;

	private final ThreadLocal<ThreadLocalData> data = new ThreadLocal<ThreadLocalData>();
	// Max value for commit-counter, values over this limit will be logged.
	private static int maxCommitCounter = 10;

	/**
	 * [used by spring]
	 */
	public DBImpl(Properties databaseProperties) {
		if(INSTANCE == null) {
			INSTANCE = this;
			try {
				emf = Persistence.createEntityManagerFactory("default", databaseProperties);
			} catch (Exception e) {
				// Our application is useless without DB, so fail fast and deliver a clear message
				System.err.println("Could not create EntityManagerFactory with given database properties");
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}

	/**
	 * Used by lmsuzh-extension-olatreplacement
	 *
	 * @param emf
	 */
	protected DBImpl(EntityManagerFactory emf) {
        DBImpl.emf = emf;
		INSTANCE = this;
	}
	
	protected static DBImpl getInstance() {
		return INSTANCE;
	}

	@Override
	public boolean isMySQL() {
		return "mysql".equals(dbVendor);
	}

	@Override
	public boolean isPostgreSQL() {
		return "postgresql".equals(dbVendor);
	}

	@Override
	public boolean isOracle() {
		return "oracle".equals(dbVendor);
	}

	@Override
	public String getDbVendor() {
		return dbVendor;
	}
	/**
	 * [used by spring]
	 * @param dbVendor
	 */
	public void setDbVendor(String dbVendor) {
		this.dbVendor = dbVendor;
	}

	/**
	 * A <b>ThreadLocalData</b> is used as a central place to store data on a per
	 * thread basis.
	 * 
	 * @author Andreas CH. Kapp
	 * @author Christian Guretzki
	 */
	protected class ThreadLocalData {

		private boolean error;
		private Exception lastError;
		
		private boolean initialized = false;
		// count number of db access in beginTransaction, used to log warn 'to many db access in one transaction'
		private int accessCounter = 0;
		// count number of commit in db-session, used to log warn 'Call more than one commit in a db-session'
		private int commitCounter = 0;
		
		private EntityManager em;
		
		private ThreadLocalData() {
		// don't let any other class instantiate ThreadLocalData.
		}
		
		public EntityManager getEntityManager(boolean createIfNecessary) {
			if(em == null && createIfNecessary) {
				em = emf.createEntityManager();
			}
			return em;
		}
		
		public EntityManager renewEntityManager() {
			if(em != null && !em.isOpen()) {
				try {
					em.close();
				} catch (Exception e) {
					log.error("", e);
				}
				em = null;
			}
			return getEntityManager(true);
		}
		
		public void removeEntityManager() {
			em = null;
		}
		
		public boolean hasTransaction() {
			if(em != null && em.isOpen()) {
				EntityTransaction trx = em.getTransaction();
				return trx != null && trx.isActive();
			}
			return false;
		}

		/**
		 * @return true if initialized.
		 */
		protected boolean isInitialized() {
			return initialized;
		}

		protected void setInitialized(boolean b) {
			initialized = b;
		}

		public boolean isError() {
			if(em != null && em.isOpen()) {
				EntityTransaction trx = em.getTransaction();
				if (trx != null && trx.isActive()) {
					return trx.getRollbackOnly();
				} 
			}
			return error;
		}

		public void setError(boolean error) {
			this.error = error;
		}

		public Exception getLastError() {
			return lastError;
		}

		public void setError(Exception ex) {
			this.lastError = ex;
			this.error = true;
		}

		protected void incrementAccessCounter() {
			this.accessCounter++;
		}
		
		protected int getAccessCounter() {
			return this.accessCounter;
		}
		
		protected void resetAccessCounter() {
			this.accessCounter = 0;
		}	

		protected void incrementCommitCounter() {
			this.commitCounter++;
		}
		
		protected int getCommitCounter() {
			return this.commitCounter;
		}

		protected void resetCommitCounter() {
			this.commitCounter = 0;
		}
	}

	private void setData(ThreadLocalData data) {
		this.data.set(data);
	}

	private ThreadLocalData getData() {
		ThreadLocalData tld = data.get();
		if (tld == null) {
			tld = new ThreadLocalData();
			setData(tld);
		}
		return tld;
	}
	
	@Override
	public EntityManager getCurrentEntityManager() {
		//if spring has already an entity manager in this thread bounded, return it
		EntityManager threadBoundedEm = getData().getEntityManager(true);
		if(threadBoundedEm != null && threadBoundedEm.isOpen()) {
			EntityTransaction trx = threadBoundedEm.getTransaction();
			//if not active begin a new one (possibly manual committed)
			if(!trx.isActive()) {
				trx.begin();
			}
			updateDataStatistics("entityManager");
			return threadBoundedEm;
		} else if(threadBoundedEm == null || !threadBoundedEm.isOpen()) {
			threadBoundedEm = getData().renewEntityManager();
		}
		
		EntityTransaction trx = threadBoundedEm.getTransaction();
		//if not active begin a new one (possibly manual committed)
		if(!trx.isActive()) {
			trx.begin();
		}
		updateDataStatistics("entityManager");
		return threadBoundedEm;
	}
	
	private Session getSession(EntityManager em) {
		return em.unwrap(HibernateEntityManager.class).getSession();
	}
	
	private boolean unusableTrx(EntityTransaction trx) {
		return trx == null || !trx.isActive() || trx.getRollbackOnly();
	}
	
	private void updateDataStatistics(Object logObject) {
		if (getData().getAccessCounter() > MAX_DB_ACCESS_COUNT) {
			log.warn("beginTransaction bulk-change, too many db access for one transaction, could be a performance problem (add closeSession/createSession in loop) logObject=" + logObject, null);
			getData().resetAccessCounter();
		} else {
  			getData().incrementAccessCounter();
    	}
    }

	/**
	 * Close the database session.
	 */
	@Override
	public void closeSession() {
		getData().resetAccessCounter();
		// Note: closeSession() now also checks if the connection is open at all
		//  in OLAT-4318 a situation is described where commit() fails and closeSession()
		//  is not called at all. that was due to a call to commit() with a session
		//  that was closed underneath by hibernate (not noticed by DBImpl).
		//  in order to be robust for any similar situation, we check if the 
		//  connection is open, otherwise we shouldn't worry about doing any commit/rollback anyway
		

		//commit
		//getCurrentEntityManager();
		EntityManager s = getData().getEntityManager(false);
		if(s != null) {
			EntityTransaction trx = s.getTransaction();
			if(trx.isActive()) {
				try {
					trx.commit();
				} catch (RollbackException ex) {
					//possible if trx setRollbackonly
					log.warn("Close session with transaction set with setRollbackOnly", ex);
				} catch (Exception e) {
					log.error("", e);
					trx.rollback();
				}
			}
			s.close();
		}
		data.remove();
	}
  
	private boolean contains(Object object) {
		EntityManager em = getCurrentEntityManager();
		return em.contains(object);
	}

	/**
	 * Create a DBQuery
	 * 
	 * @param query
	 * @return DBQuery
	 */
	@Override
	public DBQuery createQuery(String query) {
		try {
			EntityManager em = getCurrentEntityManager();
			Query q = getSession(em).createQuery(query);
			return new DBQueryImpl(q);
		} catch (HibernateException he) {
			getData().setError(he);
			throw new DBRuntimeException("Error while creating DBQueryImpl: ", he);
		}
	}

	/**
	 * Delete an object.
	 * 
	 * @param object
	 */
	@Override
	public void deleteObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + object);
		}
		try {
			Object relaoded = em.merge(object);
			em.remove(relaoded);
			if (log.isDebug()) {
				log.debug("delete (trans "+trx.hashCode()+") class "+object.getClass().getName()+" = "+object.toString());	
			}
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Delete of object failed: " + object, e);
		}
	}

	/**
	 * Deletion query.
	 * 
	 * @param query
	 * @param value
	 * @param type
	 * @return nr of deleted rows
	 */
	@Override
	public int delete(String query, Object value, Type type) {
		int deleted = 0;
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + value);
		}
		try {
			//old: deleted = getSession().delete(query, value, type);
			Session si = getSession(em);
			Query qu = si.createQuery(query);
			qu.setParameter(0, value, type);
			List foundToDel = qu.list();
			int deletionCount = foundToDel.size();
			for (int i = 0; i < deletionCount; i++ ) {
				si.delete( foundToDel.get(i) );
			}
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			throw new DBRuntimeException ("Could not delete object: " + value, e);
		}
		return deleted;
	}

	/**
	 * Deletion query.
	 * 
	 * @param query
	 * @param values
	 * @param types
	 * @return nr of deleted rows
	 */
	@Override
	public int delete(String query, Object[] values, Type[] types) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot delete in a transaction that is rolledback or committed " + values);
		}
		try {
			//old: deleted = getSession().delete(query, values, types);
			Session si = getSession(em);
			Query qu = si.createQuery(query);
			qu.setParameters(values, types);
			List foundToDel = qu.list();
			int deleted = foundToDel.size();
			for (int i = 0; i < deleted; i++ ) {
				si.delete( foundToDel.get(i) );
			}	
			return deleted;
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			throw new DBRuntimeException ("Could not delete object: " + values, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @param value
	 * @param type
	 * @return List of results.
	 */
	@Override
	public List find(String query, Object value, Type type) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		try {
			Query qu = getSession(em).createQuery(query);
			qu.setParameter(0, value, type);
			return qu.list();
		} catch (HibernateException e) {
			trx.setRollbackOnly();
			String msg = "Find failed in transaction. Query: " +  query + " " + e;
			getData().setError(e);
			throw new DBRuntimeException(msg, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @param values
	 * @param types
	 * @return List of results.
	 */
	@Override
	public List find(String query, Object[] values, Type[] types) {
		EntityManager em = getCurrentEntityManager();
		try {
			// old: li = getSession().find(query, values, types);
			Query qu = getSession(em).createQuery(query);
			qu.setParameters(values, types);
			return qu.list();
		} catch (HibernateException e) {
			em.getTransaction().setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Find failed in transaction. Query: " +  query + " " + e, e);
		}
	}

	/**
	 * Find objects based on query
	 * 
	 * @param query
	 * @return List of results.
	 */
	@Override
	public List find(String query) {
		EntityManager em = getCurrentEntityManager();
		try {
			return em.createQuery(query).getResultList();
		} catch (HibernateException e) {
			em.getTransaction().setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Find in transaction failed: " + query + " " + e, e);
		}
	}

	/**
	 * Find an object.
	 * 
	 * @param theClass
	 * @param key
	 * @return Object, if any found. Null, if non exist. 
	 */
	@Override
	public <U> U findObject(Class<U> theClass, Long key) {
		return getCurrentEntityManager().find(theClass, key);
	}
	
	/**
	 * Load an object.
	 * 
	 * @param theClass
	 * @param key
	 * @return Object.
	 */
	@Override
	public <U> U loadObject(Class<U> theClass, Long key) {
		try {
			return getCurrentEntityManager().find(theClass, key);
		} catch (Exception e) {
			throw new DBRuntimeException("loadObject error: " + theClass + " " + key + " ", e);
		}
	}

	/**
	 * Save an object.
	 * 
	 * @param object
	 */
	@Override
	public void saveObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot save in a transaction that is rolledback or committed: " + object);
		}
		try {
			em.persist(object);					
		} catch (Exception e) { // we have some error
			e.printStackTrace(); 
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Save failed in transaction. object: " +  object, e);
		}
	}

	/**
	 * Update an object.
	 * 
	 * @param object
	 */
	@Override
	public void updateObject(Object object) {
		EntityManager em = getCurrentEntityManager();
		EntityTransaction trx = em.getTransaction();
		if (unusableTrx(trx)) { // some program bug
			throw new DBRuntimeException("cannot update in a transaction that is rolledback or committed " + object);
		}
		try {
			getSession(em).update(object);								
		} catch (HibernateException e) { // we have some error
			trx.setRollbackOnly();
			getData().setError(e);
			throw new DBRuntimeException("Update object failed in transaction. Query: " +  object, e);
		}
	}

	/**
	 * Get any errors from a previous DB call.
	 * 
	 * @return Exception, if any.
	 */
	public Exception getError() {
		return getData().getLastError();
	}

	/**
	 * @return True if any errors occured in the previous DB call.
	 */
	@Override
	public boolean isError() {
		return getData().isError();
	}

	private boolean hasTransaction() {
		return getData().hasTransaction();
	}

	/**
	 * see DB.loadObject(Persistable persistable, boolean forceReloadFromDB)
	 * 
	 * @param persistable
	 * @return the loaded object
	 */
	@Override
	public Persistable loadObject(Persistable persistable) {
		return loadObject(persistable, false);
	}

	/**
	 * loads an object if needed. this makes sense if you have an object which had
	 * been generated in a previous hibernate session AND you need to access a Set
	 * or a attribute which was defined as a proxy.
	 * 
	 * @param persistable the object which needs to be reloaded
	 * @param forceReloadFromDB if true, force a reload from the db (e.g. to catch
	 *          up to an object commited by another thread which is still in this
	 *          thread's session cache
	 * @return the loaded Object
	 */
	@Override
	public Persistable loadObject(Persistable persistable, boolean forceReloadFromDB) {
		if (persistable == null) throw new AssertException("persistable must not be null");

		EntityManager em = getCurrentEntityManager();
		Class<? extends Persistable> theClass = persistable.getClass();
		if (forceReloadFromDB) {
			// we want to reload it from the database.
			// there are 3 scenarios possible:
			// a) the object is not yet in the hibernate cache
			// b) the object is in the hibernate cache
			// c) the object is detached and there is an object with the same id in the hibernate cache
			
			if (contains(persistable)) {
				// case b - then we can use evict and load
				evict(em, persistable, getData());
				return loadObject(theClass, persistable.getKey());
			} else {
				// case a or c - unfortunatelly we can't distinguish these two cases
				// and session.refresh(Object) doesn't work.
				// the only scenario that works is load/evict/load
				Persistable attachedObj = loadObject(theClass, persistable.getKey());
				evict(em, attachedObj, getData());
				return loadObject(theClass, persistable.getKey());
			}
		} else if (!contains(persistable)) { 
			// forceReloadFromDB is false - hence it is OK to take it from the cache if it would be there
			// now this object directly is not in the cache, but it's possible that the object is detached
			// and there is an object with the same id in the hibernate cache.
			// therefore the following loadObject can either return it from the cache or load it from the DB
			return loadObject(theClass, persistable.getKey());
		} else { 
			// nothing to do, return the same object
			return persistable;
		}
	}
	
	private void evict(EntityManager em, Object object, ThreadLocalData localData) {
		try {
			getSession(em).evict(object);			
		} catch (Exception e) {
			localData.setError(e);
			throw new DBRuntimeException("Error in evict() Object from Database. ", e);
		}
	}

	@Override
	public void commitAndCloseSession() {
		try {
			commit();
		} finally {
			try{
				// double check: is the transaction still open? if yes, is it not rolled-back? if yes, do a rollback now!
				if (hasTransaction() && isError()) {
					log.error("commitAndCloseSession: commit seems to have failed, transaction still open. Doing a rollback!", new Exception("commitAndCloseSession"));
					rollback();
				}
			} finally {
				closeSession();
			}
		}
	}
	
	@Override
	public void rollbackAndCloseSession() {
		try {
			rollback();
		} finally {
			closeSession();
		}
	}

	/**
	 * Call this to commit a transaction opened by beginTransaction().
	 */
	@Override
	public void commit() {
		boolean debug = log.isDebug();
		if (debug) log.debug("commit start...", null);
		try {
			if (hasTransaction() && !isError()) {
				if (debug) log.debug("has Transaction and is in Transaction => commit", null);
				getData().incrementCommitCounter();
				if (debug) {
					if ((maxCommitCounter != 0) && (getData().getCommitCounter() > maxCommitCounter) ) {
						log.info("Call too many commit in a db-session, commitCounter=" + getData().getCommitCounter() +"; could be a performance problem" , null);
					}
				}
				
				EntityTransaction trx = getCurrentEntityManager().getTransaction();
				if(trx != null) {
					trx.commit();
				}

				if (debug) log.debug("Commit DONE hasTransaction()=" + hasTransaction(), null);
			} else if(hasTransaction() && isError()) {
				EntityTransaction trx = getCurrentEntityManager().getTransaction();
				if(trx != null && trx.isActive()) {
					throw new DBRuntimeException("Try to commit a transaction in error status");
				}
			} else {
				if (debug) log.debug("Call commit without starting transaction", null );
			}
		} catch (Error er) {
			log.error("Uncaught Error in DBImpl.commit.", er);
			throw er;
		} catch (Exception e) {
			// Filter Exception form async TaskExecutorThread, there are exception allowed
			if (!Thread.currentThread().getName().equals("TaskExecutorThread")) {
				log.warn("Caught Exception in DBImpl.commit.", e);
			}
			// Error when trying to commit
			try {
				if (hasTransaction()) {
					EntityTransaction trx = getCurrentEntityManager().getTransaction();
					if(trx != null && trx.isActive()) {
						if(trx.getRollbackOnly()) {
							try {
								trx.commit();
							} catch (RollbackException e1) {
								//we wait for this exception
							}
						} else {
							trx.rollback();
						}
					}
				}
			} catch (Error er) {
				log.error("Uncaught Error in DBImpl.commit.catch(Exception).", er);
				throw er;
			} catch (Exception ex) {
				log.warn("Could not rollback transaction after commit!", ex);
				throw new DBRuntimeException("rollback after commit failed", e);
			}
			throw new DBRuntimeException("commit failed, rollback transaction", e);
		}
	}
	
	/**
	 * Call this to rollback current changes.
	 */
	@Override
	public void rollback() {
		if (log.isDebug()) log.debug("rollback start...", null);
		try {
			// see closeSession() and OLAT-4318: more robustness with commit/rollback/close, therefore
			// we check if the connection is open at this stage at all
			EntityTransaction trx = getCurrentEntityManager().getTransaction();
			if(trx != null && trx.isActive()) {
				if(trx.getRollbackOnly()) {
					try {
						trx.commit();
					} catch (RollbackException e) {
						//we wait for this exception
					}
				} else {
					trx.rollback();
				}
			}

		} catch (Exception ex) {
			log.warn("Could not rollback transaction!",ex);
			throw new DBRuntimeException("rollback failed", ex);
		}		
	}

	/**
	 * Statistics must be enabled first, when you want to use it. 
	 * @return Return Hibernates statistics object.
	 */
	@Override
	public Statistics getStatistics() {
		if(emf instanceof HibernateEntityManagerFactory) {
			return ((HibernateEntityManagerFactory)emf).getSessionFactory().getStatistics();
		}
 		return null;
   }

	@Override
	public EmbeddedCacheManager getCacheContainer() {
		EmbeddedCacheManager cm;
		try {
			Cache cache = emf.getCache();
			JpaInfinispanRegionFactory region = cache.unwrap(JpaInfinispanRegionFactory.class);
			cm = region.getCacheManager();
		} catch (Exception e) {
			log.error("", e);
			cm = null;
		}
		return cm;
	}

	/**
	 * @see org.olat.core.commons.persistence.DB#intermediateCommit()
	 */
	@Override
	public void intermediateCommit() {
		commit();
		closeSession();
	}

	@Override
	public void destroy() {
		//clean up registered drivers to prevent messages like
		// The web application [/olat] registered the JBDC driver [com.mysql.Driver] but failed to unregister...
		Enumeration<Driver> registeredDrivers = DriverManager.getDrivers();
		while(registeredDrivers.hasMoreElements()) {
			try {
				DriverManager.deregisterDriver(registeredDrivers.nextElement());
			} catch (SQLException e) {
				log.error("Could not unregister database driver.", e);
			}
		}
	}

	private void closeEntityManagerAndRemoveThreadLocalData(EntityManager entityManager) {
		getData().resetAccessCounter();
		if (entityManager != null) {
			entityManager.close();
		}
		data.remove();
	}

	@Override
	public void commitTransactionAndCloseEntityManager() {
		EntityManager entityManager = getCurrentEntityManager();
		entityManager.getTransaction().commit();
		closeEntityManagerAndRemoveThreadLocalData(entityManager);
	}

	@Override
	public void rollbackTransactionAndCloseEntityManager() {
		EntityManager entityManager = getCurrentEntityManager();
		EntityTransaction transaction = entityManager.getTransaction();
		if (transaction != null && transaction.isActive()) {
			transaction.rollback();
		}
		closeEntityManagerAndRemoveThreadLocalData(entityManager);
	}

	@Override
	public void flush() {
		getCurrentEntityManager().flush();
	}

	@Override
	public void clear() {
		getCurrentEntityManager().clear();
	}
}
