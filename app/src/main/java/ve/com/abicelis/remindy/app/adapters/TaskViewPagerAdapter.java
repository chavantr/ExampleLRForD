package ve.com.abicelis.remindy.app.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.List;



public class TaskViewPagerAdapter extends FragmentPagerAdapter {

    //DATA
    private List<String> mTitleList;
    private List<Fragment> mFragmentList;                                           //Holds all of the fragments
    private SparseArray<Fragment> mRegisteredFragmentList = new SparseArray<>();    //Holds only registered fragments


    public TaskViewPagerAdapter(FragmentManager fm, List<String> titleList, List<Fragment> fragmentList) {
        super(fm);
        mTitleList = titleList;
        mFragmentList = fragmentList;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitleList.get(position);
    }




    /* These methods are for maintaining and getting registered fragments */
    public Fragment getRegisteredFragment(int position) {
        return mRegisteredFragmentList.get(position);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        mRegisteredFragmentList.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        mRegisteredFragmentList.remove(position);
        super.destroyItem(container, position, object);
    }
}
