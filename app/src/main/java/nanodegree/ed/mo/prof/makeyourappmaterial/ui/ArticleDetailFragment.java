package nanodegree.ed.mo.prof.makeyourappmaterial.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ShareCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;

import butterknife.BindView;
import butterknife.ButterKnife;
import nanodegree.ed.mo.prof.makeyourappmaterial.R;
import nanodegree.ed.mo.prof.makeyourappmaterial.data.ArticleLoader;

import static nanodegree.ed.mo.prof.makeyourappmaterial.ui.ArticleListActivity.EXTRA_CURRENT_ARTICLE_POSITION;
import static nanodegree.ed.mo.prof.makeyourappmaterial.ui.ArticleListActivity.EXTRA_STARTING_ARTICLE_POSITION;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";

    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private int mCurrentPosition, mStartingPosition, mTopInset;
//    private boolean mIsTransitioning;
    private boolean mIsCard = false;
//    private long mBackgroundImageFadeMillis;

    ProgressDialog mProgressDialog;
    @BindView(R.id.scrollView)
    NestedScrollView mScrollView;
    @BindView(R.id.photo_container)
    View mPhotoContainerView;
    @BindView(R.id.thumbnail)
    ImageView mPhotoView;
    @Nullable @BindView(R.id.appbar)
    AppBarLayout appBarLayout;
    @BindView(R.id.share_fab)
    FloatingActionButton mShareFab;
    @BindView(R.id.title_bar)
    LinearLayout titleBar;
    @BindView(R.id.article_title)
    TextView titleView;
    @BindView(R.id.article_byline)
    TextView bylineView;
    @BindView(R.id.article_body)
    TextView bodyView;
    @BindView(R.id.detail_toolbar)
    Toolbar detailToolbar;
    Date publishedDate;
    Spanned spanned;
    //    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
//    private ColorDrawable mStatusBarColorDrawable;
//    private int mScrollY;
//    private int mStatusBarFullOpacityBottom;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId, int position, int startingPosition) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
        mProgressDialog = ProgressDialog.show(getActivity(), getString(R.string.loading_msg), getString(R.string.pls_wait), true);
        mStartingPosition=getArguments().getInt(EXTRA_STARTING_ARTICLE_POSITION);
        mCurrentPosition=getArguments().getInt(EXTRA_CURRENT_ARTICLE_POSITION);
//        mIsTransitioning=savedInstanceState==null&&mStartingPosition==mCurrentPosition;
//        mBackgroundImageFadeMillis=getResources().getInteger(
//                R.integer.fragment_details_background_image_fade_millis
//        );
        if (Build.VERSION.SDK_INT >= 21) {
            getActivity().getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
//        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
//                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        ButterKnife.bind(this, mRootView);
        ((AppCompatActivity) getActivity()).setSupportActionBar(detailToolbar);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowTitleEnabled(false);
        bindViews();
        return mRootView;
    }

//    public void startPostponedEnterTransition() {
//        if (mCurrentPosition == mStartingPosition) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                mPhotoView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//                    @Override
//                    public boolean onPreDraw() {
//                        mPhotoView.getViewTreeObserver().removeOnPreDrawListener(this);
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                            getActivity().startPostponedEnterTransition();
//                        }
//                        return true;
//                    }
//                });
//            }
//        }
//    }

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

    private void bindViews() {
//        if (mRootView == null) {
//            return;
//        }
        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            final String Title= mCursor.getString(ArticleLoader.Query.TITLE);
            titleView.setText(Title);

            publishedDate = parsePublishedDate();

            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                spanned= getTimeFromHTML();
                bylineView.setText(spanned.toString());
            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }
            final String body=mCursor.getString(ArticleLoader.Query.BODY);
            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                                startPostponedEnterTransition();
                            }
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                if (appBarLayout!=null){
                                    titleBar.setBackgroundColor(mMutedColor);
                                }
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                        }
                    });
            mShareFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                            .setType("text/plain")
                            .setText(Title+ "/n" + publishedDate+ "/n" + body)
                            .getIntent(), getString(R.string.action_share)));
                }
            });
            mProgressDialog.dismiss();
        }
//        else {
//            mRootView.setVisibility(View.GONE);
//            titleView.setText("N/A");
//            bylineView.setText("N/A" );
//            bodyView.setText("N/A");
//        }
    }

    private Spanned getTimeFromHTML() {
        return  Html.fromHtml(
                DateUtils.getRelativeTimeSpanString(
                        publishedDate.getTime(),
                        System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_ALL).toString()
                        + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                        + "</font>");
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }
        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

//    @Nullable
//    ImageView getAlbumImage(){
//        if (isViewInBounds(getActivity().getWindow().getDecorView(), mPhotoView)) {
//            return mPhotoView;
//        }
//        return null;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Intent intent = new Intent(getActivity(), ArticleListActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    getActivity().finish();
                    return true;
//                }
        }
        return super.onOptionsItemSelected(item);
    }

//    private static boolean isViewInBounds(@NonNull View container, @NonNull View view) {
//        Rect containerBounds = new Rect();
//        container.getHitRect(containerBounds);
//        return view.getLocalVisibleRect(containerBounds);
//    }
}