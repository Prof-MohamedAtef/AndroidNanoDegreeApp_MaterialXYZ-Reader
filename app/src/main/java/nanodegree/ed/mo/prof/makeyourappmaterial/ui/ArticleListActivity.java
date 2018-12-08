package nanodegree.ed.mo.prof.makeyourappmaterial.ui;

import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.app.SharedElementCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import nanodegree.ed.mo.prof.makeyourappmaterial.R;
import nanodegree.ed.mo.prof.makeyourappmaterial.data.ArticleLoader;
import nanodegree.ed.mo.prof.makeyourappmaterial.data.ItemsContract;
import nanodegree.ed.mo.prof.makeyourappmaterial.data.UpdaterService;
import nanodegree.ed.mo.prof.makeyourappmaterial.remote.Config;
import nanodegree.ed.mo.prof.makeyourappmaterial.ui.Listeners.SnackBarLauncher;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener, SnackBarLauncher{

    private static final String TAG = ArticleListActivity.class.toString();
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;
    static final String EXTRA_STARTING_ARTICLE_POSITION = "extra_starting_item_position";
    static final String EXTRA_CURRENT_ARTICLE_POSITION = "extra_current_item_position";
    private Bundle mReenterState;
    public boolean mIsRefreshing=false;
//    public static boolean mIsDetailsActivityStarted;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);



//    @SuppressWarnings("NewApi")
//    private final SharedElementCallback mCallback=new SharedElementCallback() {
//        @Override
//        public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
//            super.onMapSharedElements(names, sharedElements);
//            if (mReenterState!=null){
//                // activity not exists
//                int startingPosition = mReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
//                int currentPosition = mReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION );
//                if (startingPosition != currentPosition) {
//                    String newTransitionName = getString(R.string.transition_photo) + currentPosition;
//                    View newSharedElement = mRecyclerView.findViewWithTag(newTransitionName);
//                    if (newSharedElement != null) {
//                        names.clear();
//                        names.add(newTransitionName);
//                        sharedElements.clear();
//                        sharedElements.put(newTransitionName, newSharedElement);
//                    }
//                }
//
//                mReenterState=null;
//            }else {
//                // Activity is still exists
//                View navigationBar = findViewById(android.R.id.navigationBarBackground);
//                View statusBar = findViewById(android.R.id.statusBarBackground);
//                if (navigationBar != null) {
//                    names.add(navigationBar.getTransitionName());
//                    sharedElements.put(navigationBar.getTransitionName(), navigationBar);
//                }
//                if (statusBar != null) {
//                    names.add(statusBar.getTransitionName());
//                    sharedElements.put(statusBar.getTransitionName(), statusBar);
//                }
//            }
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            setExitSharedElementCallback(mCallback);
//        }


        mSwipeRefreshLayout.setOnRefreshListener(this);
        getLoaderManager().initLoader(0, null, this);

        Config.mContext=getApplicationContext();
        Config.mCoordinatorLayout=coordinatorLayout;

        if (savedInstanceState == null) {
            refresh();
        }

        IntentFilter filter = new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE);
        filter.addAction(UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY);
        LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshingReceiver,
                filter);
    }

    private void refresh() {
        Config.mContext.startService(new Intent(Config.mContext, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
//                mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
            }else if (UpdaterService.BROADCAST_ACTION_NO_CONNECTIVITY.equals(intent.getAction())){
                mSwipeRefreshLayout.setRefreshing(false);
//                Toast.makeText(ArticleListActivity.this, getString(R.string.noconnection), Toast.LENGTH_LONG).show();
            }
        }
    };

    private void SnackBarInitializer() {
        Snackbar snackbar = Snackbar
                .make(Config.mCoordinatorLayout, Config.mContext.getResources().getString(R.string.noconnection), Snackbar.LENGTH_LONG)
                .setAction(Config.mContext.getResources().getString(R.string.retry), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        refresh();
                    }
                });
        // Changing message text color
        snackbar.setActionTextColor(Color.RED);
        // Changing action button text color
        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        mIsDetailsActivityStarted = false;
//    }

    @Override
    public void onRefresh() {
        Log.i(TAG, "onRefresh called from SwipeRefreshLayout");
        refresh();
        mSwipeRefreshLayout.setRefreshing(false);
    }

//    @Override
//    public void onActivityReenter(int resultCode, Intent data) {
//        super.onActivityReenter(resultCode, data);
//        mReenterState=new Bundle(data.getExtras());
//        int startingPosition = mReenterState.getInt(EXTRA_STARTING_ARTICLE_POSITION);
//        int currentPosition = mReenterState.getInt(EXTRA_CURRENT_ARTICLE_POSITION);
//        if (startingPosition != currentPosition) {
//            mRecyclerView.scrollToPosition(currentPosition);
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            postponeEnterTransition();
//            mRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                @Override
//                public boolean onPreDraw() {
//                    mRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                        mRecyclerView.requestLayout();
//                        startPostponedEnterTransition();
//                        return true;
//                    }
//                    return false;
//                }
//            });
//        }
//    }

//    public static boolean getmIsDetailsActivityStarted(){
//        return mIsDetailsActivityStarted;
//    }

    @Override
    public void onNoInternetConnection() {
        SnackBarInitializer();
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}