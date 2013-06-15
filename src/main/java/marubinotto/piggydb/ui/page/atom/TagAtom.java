package marubinotto.piggydb.ui.page.atom;

import java.util.List;

import marubinotto.piggydb.model.Fragment;
import marubinotto.piggydb.model.Tag;
import marubinotto.piggydb.model.entity.RawFilter;
import marubinotto.piggydb.model.query.FragmentsByFilter;
import marubinotto.piggydb.ui.page.common.AbstractBorderPage;

public class TagAtom extends AbstractAtom {
	
	public Long id;
	
	private Tag tag;
	
	@Override
	protected void setFeedInfo() throws Exception {
		super.setFeedInfo();
		
		if (this.id == null) return;
		
		this.feedId = this.feedId + PARAM_PREFIX_IN_ID + this.id;
		appendQueryToUrls("?id=" + this.id);
			
		this.tag = getDomain().getTagRepository().get(this.id.longValue());
		if (this.tag == null) return;
		
		this.feedTitle = this.feedTitle + AbstractBorderPage.HTML_TITLE_SEP + this.tag.getName();
	}

	@Override
	protected List<Fragment> getFragments() throws Exception {
		if (this.tag == null) return null;
		
		RawFilter filter = new RawFilter();
		filter.getIncludes().addTag(this.tag);
		
		FragmentsByFilter query = (FragmentsByFilter)getQuery(FragmentsByFilter.class);
		query.setFilter(filter);
		return getPage(query);
	}
}
