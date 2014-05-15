package marubinotto.piggydb.service;

import java.util.List;

import marubinotto.piggydb.model.FileRepository;
import marubinotto.piggydb.model.FilterRepository;
import marubinotto.piggydb.model.Fragment;
import marubinotto.piggydb.model.FragmentRepository;
import marubinotto.piggydb.model.GlobalSetting;
import marubinotto.piggydb.model.Tag;
import marubinotto.piggydb.model.TagRepository;
import marubinotto.piggydb.model.auth.Authentication;
import marubinotto.piggydb.model.auth.User;
import marubinotto.util.Assert;
import marubinotto.util.procedure.Procedure;
import marubinotto.util.procedure.Transaction;

import org.springframework.beans.factory.BeanFactory;

public class DomainModelBeans {

	private BeanFactory factory;

	public DomainModelBeans(BeanFactory factory) {
		Assert.Arg.notNull(factory, "factory");
		this.factory = factory;
	}
	
	public Transaction getTransaction() {
  	return (Transaction)this.factory.getBean("transaction");
  }

	public GlobalSetting getGlobalSetting() {
		return (GlobalSetting)this.factory.getBean("globalSetting");
	}

	public Authentication getAuthentication() {
		return (Authentication)this.factory.getBean("authentication");
	}
  
	public TagRepository getTagRepository() {
  	return (TagRepository)this.factory.getBean("tagRepository");
  }
  
	public FragmentRepository getFragmentRepository() {
  	return (FragmentRepository)this.factory.getBean("fragmentRepository");
  }
  
	public FilterRepository getFilterRepository() {
  	return (FilterRepository)this.factory.getBean("filterRepository");
  }
  
	public FileRepository getFileRepository() {
  	return (FileRepository)this.factory.getBean("fileRepository");
  }
	
	public void saveFragment(final Fragment fragment, User user) throws Exception {
		fragment.validateAsTag(user, getTagRepository());
		getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				getFragmentRepository().update(fragment);
				return null;
			}
		});
	}
	
	public void saveTag(final Tag tag, final User user) throws Exception {
		getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				getFragmentRepository().update(tag, user);
				return null;
			}
		});
	}
	
	public Fragment deleteTag(final Tag tag, final User user) throws Exception {
		return (Fragment)getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				return getFragmentRepository().delete(tag, user);
			}
		});
	}
	
	public void registerFragmentIfNotExists(final Tag tag, final User user) 
	throws Exception {
		getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				getFragmentRepository().registerFragmentIfNotExists(tag, user);
				return null;
			}
		});
	}
	
	public void addTagToFragments(
		final List<Fragment> fragments, 
		final Tag tag, 
		final User user)
	throws Exception {
		getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				for (Fragment fragment : fragments) {
					fragment.addTagByUser(tag, user);
					fragment.validateAsTag(user, getTagRepository());
					getFragmentRepository().update(fragment);
				}
				return null;
			}
		});
	}
	
	public void removeTagFromFragments(
		final List<Fragment> fragments, 
		final String tagName,
		final User user)
	throws Exception {
		getTransaction().execute(new Procedure() {
			public Object execute(Object input) throws Exception {
				for (Fragment fragment : fragments) {
					fragment.removeTagByUser(tagName, user);
					fragment.validateAsTag(user, getTagRepository());
					getFragmentRepository().update(fragment);
				}
				return null;
			}
		});
	}
}
