package initiativaromania.hartabanilorpublici.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedList;

import initiativaromania.hartabanilorpublici.R;
import initiativaromania.hartabanilorpublici.comm.CommManager;
import initiativaromania.hartabanilorpublici.comm.CommManagerResponse;
import initiativaromania.hartabanilorpublici.data.Company;
import initiativaromania.hartabanilorpublici.data.Contract;
import initiativaromania.hartabanilorpublici.data.PublicInstitution;

import com.github.aakira.expandablelayout.ExpandableRelativeLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InstitutionFragment extends Fragment implements TabbedViewPageListener{
    public static final int CONTRACT_LIST_FOR_COMPANY               = 1;
    public static final int CONTRACT_LIST_FOR_PUBLIC_INSTITUTION    = 2;
    public static final int CONTRACT_LIST_FOR_SEARCH                = 3;

    public static final String CONTRACT_LIST_TYPE = "contract list type";
    public static final String CONTRACT_LIST_EXTRA = "contract list extra";
    public static final int DIRECT_ACQ_FRAGMENT_INDEX               = 0;
    public static final int TENDER_FRAGMENT_INDEX                   = 1;
    public static final int INSTITUTIONS_FRAGMENT_INDEX             = 2;

    private int type = CONTRACT_LIST_FOR_COMPANY;

    /* The root public institution for this view */
    private PublicInstitution pi;

    /* The root company for this view */
    private Company company;

    private ContractListFragment directAcqListFragment;
    private ContractListFragment tendersListFragment;
    private CompanyListFragment companyListFragment;
    private InstitutionListFragment piListFragment;

    TabbedViewPageFragment viewPageFragment;

    private View originalView;
    private Fragment fragmentCopy;

    private LinkedList<Contract> directAcqs = new LinkedList<Contract>();
    private LinkedList<Contract> tenders = new LinkedList<Contract>();
    private LinkedList<Company> companies = new LinkedList<Company>();
    private LinkedList<PublicInstitution> pis = new LinkedList<PublicInstitution>();

    public String oldTitle;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /* Inflate the layout for this fragment */
        originalView = inflater.inflate(R.layout.fragment_institution, container, false);
        fragmentCopy = this;

        /* Init the expandable layout */
        LinearLayout layout = (LinearLayout) originalView.findViewById(R.id.layoutPIName);
        ExpandableRelativeLayout expandableLayout1 = (ExpandableRelativeLayout) originalView
                .findViewById(R.id.expandableLayout1);
        layout.setOnClickListener(new LinearLayout.OnClickListener() {
            public void onClick(View v) {
                System.out.println("Expanding view");
                ExpandableRelativeLayout expandableLayout1 = (ExpandableRelativeLayout) getView()
                        .findViewById(R.id.expandableLayout1);
                expandableLayout1.toggle(); // toggle expand and collapse
            }
        });


        /* Build the View Pager */
        viewPageFragment = (TabbedViewPageFragment)
                getChildFragmentManager().findFragmentById(R.id.entity_info_fragment);
        viewPageFragment.registerPageListener(this);


        /* Get fragment parameters */
        Bundle bundle = new Bundle();
        bundle = getArguments();
        if (bundle == null)
            return originalView;

        type = bundle.getInt(CommManager.BUNDLE_INST_TYPE);
        System.out.println("Institution Arguments: Type" + type );

        if (type == CONTRACT_LIST_FOR_COMPANY) {
            company = new Company();
            company.id = bundle.getInt(CommManager.BUNDLE_COMPANY_ID);
            company.type = bundle.getInt(CommManager.BUNDLE_COMPANY_TYPE);
            company.name = bundle.getString(CommManager.BUNDLE_COMPANY_NAME);

            viewPageFragment.setViewPager(CONTRACT_LIST_FOR_COMPANY);

            System.out.println("Company Arguments: id " + company.id + " name " + company.name +
                    " type " + company.type);

            displayInitCompanyInfo();
        } else {
            pi = new PublicInstitution();
            pi.id = bundle.getInt(CommManager.BUNDLE_PI_ID);
            pi.name = bundle.getString(CommManager.BUNDLE_PI_NAME);
            pi.directAcqs = bundle.containsKey(CommManager.BUNDLE_PI_ACQS) ?
                    bundle.getInt(CommManager.BUNDLE_PI_ACQS) : -1;
            pi.tenders = bundle.containsKey(CommManager.BUNDLE_PI_ACQS) ?
                    bundle.getInt(CommManager.BUNDLE_PI_TENDERS) : - 1;

            viewPageFragment.setViewPager(CONTRACT_LIST_FOR_PUBLIC_INSTITUTION);
            System.out.println("Institution Arguments: id " + pi.id + " name " + pi.name +
                " acqs " + pi.directAcqs + " tenders " + pi.tenders);

            /* Show initial PI info received as arguments */
            displayInitPIInfo();
        }


        /* Call the server to get all the direct acquisitions.
         * The rest of the data will be retrieved when the tab is changed */
        getInitInfo();
        getServerTabInfo(DIRECT_ACQ_FRAGMENT_INDEX);

        return originalView;
    }


    /* Get initial information for this institution */
    public void getInitInfo() {

        System.out.println("InstitutionFragment: Get initial info");

        if (type == CONTRACT_LIST_FOR_COMPANY) {
            switch (company.type) {
                case Company.COMPANY_TYPE_AD:
                    /* Send request to get the init data */
                    CommManager.requestADCompany(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyInitInfo(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                case Company.COMPANY_TYPE_TENDER:
                    /* Send request to get the init data */
                    CommManager.requestTenderCompany(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyInitInfo(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                default:
                    System.out.println("InstitutionFragment: Unknown company type at init");
            }

        } else {

             /* Send request to get the init data for a public institution */
            CommManager.requestPIInfo(new CommManagerResponse() {
                @Override
                public void processResponse(JSONArray response) {
                    receivePIInitInfo(response);
                }

                @Override
                public void onErrorOccurred(String errorMsg) {
                    if (fragmentCopy.getContext() != null)
                        Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                Toast.LENGTH_SHORT).show();
                }
            }, pi.id);
        }
    }


    /* Get server info for a tab */
    private void getServerTabInfo(int position) {

        System.out.println("InstitutionFragment: Get Server Info for tab " + position);

        /* Call the server to fill each tab with data */
        switch (position) {
            case DIRECT_ACQ_FRAGMENT_INDEX:
                if (directAcqs == null || directAcqs.size() == 0)
                    getServerADInfo();
                break;

            case TENDER_FRAGMENT_INDEX:
                if (tenders == null || tenders.size() == 0)
                    getServerTenderInfo();
                break;

            case INSTITUTIONS_FRAGMENT_INDEX:
                if ((type == CONTRACT_LIST_FOR_COMPANY && (pis == null || pis.size() == 0)) ||
                        (type == CONTRACT_LIST_FOR_PUBLIC_INSTITUTION && (companies == null || companies.size() == 0)))
                    getServerInstitutionInfo();
                break;

            default:
                System.out.println("InstitutionFragment: Error retrieving data for tab " + position);
        }
    }


    /* Get server info for Direct Acquisitions */
    public void getServerADInfo() {

        System.out.println("InstitutionFragment: Get Server Info for ADs");

        if (type == CONTRACT_LIST_FOR_COMPANY) {
            switch (company.type) {
                case Company.COMPANY_TYPE_AD:

                    /* Send request to get all the direct acquisitions of an AD Company */
                    CommManager.requestADCompanyContracts(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyAcqs(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                case Company.COMPANY_TYPE_TENDER:
                    /* An AD Company doesn't have Tenders */
                    break;

                default:
                    System.out.println("InstitutionFragment: Unknown company type at ads");

            }

        } else {

            /* Send request to get the PI's direct acquisitions */
            CommManager.requestPIAcqs(new CommManagerResponse() {
                @Override
                public void processResponse(JSONArray response) {
                    receivePIAcqs(response);
                }

                @Override
                public void onErrorOccurred(String errorMsg) {
                    if (fragmentCopy.getContext() != null)
                        Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                Toast.LENGTH_SHORT).show();
                }
            }, pi.id);
        }
    }


    /* Get server info for Tenders */
    public void getServerTenderInfo() {

        System.out.println("InstitutionFragment: Get Server Info for Tenders");

        if (type == CONTRACT_LIST_FOR_COMPANY) {
            switch (company.type) {
                case Company.COMPANY_TYPE_AD:
                    /* A Tender Company doesn't have ADs */
                    break;

                case Company.COMPANY_TYPE_TENDER:

                    /* Send request to get all the tenders of an AD Company */
                    CommManager.requestTenderCompanyTenders(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyTenders(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                default:
                    System.out.println("InstitutionFragment: Unknown company type at tenders");

            }

        } else {
            /* Send request to get the PI's tenders */
            CommManager.requestPITenders(new CommManagerResponse() {
                @Override
                public void processResponse(JSONArray response) {
                    receivePITenders(response);
                }

                @Override
                public void onErrorOccurred(String errorMsg) {
                    if (fragmentCopy.getContext() != null)
                        Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                Toast.LENGTH_SHORT).show();
                }
            }, pi.id);
        }
    }


    /* Get server info for companies/instituions */
    public void getServerInstitutionInfo() {

        System.out.println("InstitutionFragment: Get Server Info for Companies/Institutions");

         /* Get Server information for a company */
        if (type == CONTRACT_LIST_FOR_COMPANY) {
            switch (company.type) {
                case Company.COMPANY_TYPE_AD:

                    /* Send request to get all the public institutions of an AD Company */
                    CommManager.requestPIsByADCompany(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyPIs(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                case Company.COMPANY_TYPE_TENDER:

                    /* Send request to get all the public institutions of a Tender Company */
                    CommManager.requestPIsByTenderCompany(new CommManagerResponse() {
                        @Override
                        public void processResponse(JSONArray response) {
                            receiveCompanyPIs(response);
                        }

                        @Override
                        public void onErrorOccurred(String errorMsg) {
                            if (fragmentCopy.getContext() != null)
                                Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                        Toast.LENGTH_SHORT).show();
                        }
                    }, company.id);
                    break;

                default:
                    System.out.println("InstitutionFragment: Unknown company type for institutions tab");
            }

        } else {

            /* Send request to get the institution AD companies */
            CommManager.requestADCompaniesByPI(new CommManagerResponse() {
                @Override
                public void processResponse(JSONArray response) {
                    receivePICompanies(response, Company.COMPANY_TYPE_AD);
                }

                @Override
                public void onErrorOccurred(String errorMsg) {
                    if (fragmentCopy.getContext() != null)
                        Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                Toast.LENGTH_SHORT).show();
                }
            }, pi.id);

            /* Send request to get the institution Tender companies */
            CommManager.requestTenderCompaniesByPI(new CommManagerResponse() {
                @Override
                public void processResponse(JSONArray response) {
                    receivePICompanies(response, Company.COMPANY_TYPE_TENDER);
                }

                @Override
                public void onErrorOccurred(String errorMsg) {
                    if (fragmentCopy.getContext() != null)
                        Toast.makeText(fragmentCopy.getContext(), errorMsg,
                                Toast.LENGTH_SHORT).show();
                }
            }, pi.id);
        }
    }


    /* Receive Company information from the server */
    private void receiveCompanyInitInfo(JSONArray response) {
        System.out.println("InstitutionFragment: receiveCompany " + response);

        try {
            JSONObject companySummary = response.getJSONObject(0);
            company.address = companySummary.getString(CommManager.JSON_COMPANY_ADDRESS);
            company.CUI = companySummary.getString(CommManager.JSON_COMPANY_CUI);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayServerInfo(type);
    }


    /* Receive Public Institution information from the server */
    private void receivePIInitInfo(JSONArray response) {
        System.out.println("InstitutionFragment: receivePIInitInfo " + response);

        try {
            JSONObject piSummary = response.getJSONObject(0);
            pi.CUI = piSummary.getString(CommManager.JSON_PI_CUI);
            pi.address = piSummary.getString(CommManager.JSON_PI_ADDRESS);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayServerInfo(type);
    }


    /* Receive Company Direct Acquisitions from the server */
    private void receiveCompanyAcqs(JSONArray response) {
        System.out.println("InstitutionFragment: receiveCompanyAcqs " + response +
                " size " + response.length());

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject acq = response.getJSONObject(i);
                if (acq == null)
                    continue;

                Contract a = new Contract();
                a.type = Contract.CONTRACT_TYPE_DIRECT_ACQUISITION;
                a.id = Integer.parseInt(acq.getString(CommManager.JSON_COMPANY_ACQ_ID));
                a.title = acq.getString(CommManager.JSON_CONTRACT_TITLE);
                if (type == CONTRACT_LIST_FOR_COMPANY)
                    a.company = company;
                else
                    a.pi = pi;

                directAcqs.add(a);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayDirectAcqs();
    }


    /* Receive Public Institution Tenders from the server */
    private void receiveCompanyTenders(JSONArray response) {
        System.out.println("InstitutionFragment: receiveCompanyTenders " +
                " size " + response.length());

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject tender = response.getJSONObject(i);
                if (tender == null)
                    continue;

                Contract t = new Contract();
                t.type = Contract.CONTRACT_TYPE_TENDER;
                t.id = Integer.parseInt(tender.getString(CommManager.JSON_COMPANY_TENDER_ID));
                t.title = tender.getString(CommManager.JSON_CONTRACT_TITLE);
                if (type == CONTRACT_LIST_FOR_COMPANY)
                    t.company = company;
                else
                    t.pi = pi;

                tenders.add(t);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayTenders();
    }


    /* Receive Public Institution Direct Acquisitions from the server */
    private void receivePIAcqs(JSONArray response) {
        System.out.println("InstitutionFragment: receivePIAcqs " + response +
            " size " + response.length());

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject acq = response.getJSONObject(i);
                if (acq == null)
                    continue;

                Contract a = new Contract();
                a.type = Contract.CONTRACT_TYPE_DIRECT_ACQUISITION;
                a.id = Integer.parseInt(acq.getString(CommManager.JSON_ACQ_ID));
                a.title = acq.getString(CommManager.JSON_CONTRACT_TITLE);
                a.number = acq.getString(CommManager.JSON_CONTRACT_NR);
                a.valueRON = Double.parseDouble(acq.getString(CommManager.JSON_CONTRACT_VALUE_RON));
                if (type == CONTRACT_LIST_FOR_COMPANY)
                    a.company = company;
                else
                    a.pi = pi;

                directAcqs.add(a);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayDirectAcqs();
    }


    /* Receive Public Institution Tenders from the server */
    private void receivePITenders(JSONArray response) {
        System.out.println("InstitutionFragment: receivePITenders " +
                " size " + response.length());

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject tender = response.getJSONObject(i);
                if (tender == null)
                    continue;

                Contract t = new Contract();
                t.type = Contract.CONTRACT_TYPE_TENDER;
                t.id = Integer.parseInt(tender.getString(CommManager.JSON_TENDER_ID));
                t.title = tender.getString(CommManager.JSON_CONTRACT_TITLE);
                t.number = tender.getString(CommManager.JSON_CONTRACT_NR);
                t.valueRON = Double.parseDouble(tender.getString(CommManager.JSON_CONTRACT_VALUE_RON));
                if (type == CONTRACT_LIST_FOR_COMPANY)
                    t.company = company;
                else
                    t.pi = pi;

                tenders.add(t);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayTenders();
    }


    /* Receive Companies for this institution from the server */
    private void receivePICompanies(JSONArray response, int type) {
        System.out.println("InstitutionFragment: receivePICompanies " +
                " size " + response.length() + " type " + type);

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject company = response.getJSONObject(i);
                if (company == null)
                    continue;

                Company c = new Company();
                c.type = type;
                c.id = Integer.parseInt(company.getString(CommManager.JSON_COMPANY_ID));
                c.name = company.getString(CommManager.JSON_COMPANY_NAME);

                companies.add(c);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /* Show the info received from the server */
        displayCompanies();
    }


    /* Receive Public Institutions for this company from the server */
    private void receiveCompanyPIs(JSONArray response) {
        System.out.println("InstitutionFragment: receiveCompanyPIs " +
                " size " + response.length());

        try {
            for (int i = 0; i < response.length(); i++) {
                JSONObject piObj = response.getJSONObject(i);
                if (piObj == null)
                    continue;

                PublicInstitution pi = new PublicInstitution();
                pi.id = Integer.parseInt(piObj.getString(CommManager.JSON_COMPANY_PI_ID));
                pi.name = piObj.getString(CommManager.JSON_COMPANY_PI_NAME);
                pi.CUI = piObj.getString(CommManager.JSON_COMPANY_CUI);

                pis.add(pi);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("Trying to display public institutions, size " + pis.size());

        /* Show the info received from the server */
        displayPIs();
    }


    /* PI name, acq, tenders */
    private void displayInitPIInfo() {
        oldTitle = ((HomeActivity) getActivity()).getActionBarTitle();
        ((HomeActivity) getActivity()).setActionBarTitle("Institutie Publica");


        TextView text = ((TextView) originalView.findViewById(R.id.institutionName));
        if (text != null)
            text.setText(pi.name);

        text = ((TextView) originalView.findViewById(R.id.nrDirectAcquisitions));
        if (text != null)
            text.setText(pi.directAcqs + "");

        text = ((TextView) originalView.findViewById(R.id.nrTenders));
        if (text != null)
            text.setText(pi.tenders + "");
    }


    /* Company name, address */
    private void displayInitCompanyInfo() {
        oldTitle = ((HomeActivity) getActivity()).getActionBarTitle();
        ((HomeActivity) getActivity()).setActionBarTitle("Companie");


        TextView text = ((TextView) originalView.findViewById(R.id.institutionName));
        if (text != null)
            text.setText(company.name);

        text = ((TextView) originalView.findViewById(R.id.nrDirectAcquisitions));
        if (text != null)
            text.setText(0 + "");

        text = ((TextView) originalView.findViewById(R.id.nrTenders));
        if (text != null)
            text.setText(0 + "");
    }


    /* Institution CUI, Address */
    private void displayServerInfo(int type) {
        TextView text = ((TextView) originalView.findViewById(R.id.piCUI));
        if (text != null)
            text.setText(type == CONTRACT_LIST_FOR_COMPANY ?
                    company.CUI : pi.CUI);

        text = ((TextView) originalView.findViewById(R.id.piAddress));
        if (text != null)
            text.setText(type == CONTRACT_LIST_FOR_COMPANY ?
                    company.address : pi.address);
    }


    /* Fill the list of direct acquisitions for this institution */
    private void displayDirectAcqs() {

        /* Fill the contract list fragment */
        directAcqListFragment = (ContractListFragment) viewPageFragment
                .pageAdapter.fragments.get(DIRECT_ACQ_FRAGMENT_INDEX);
        if (directAcqListFragment != null) {
            directAcqListFragment.setContracts(directAcqs);
            directAcqListFragment.displayContracts();
        } else
            System.out.println("NULL contract list fragment");
    }


    /* Fill the list of tenders for this institution */
    private void displayTenders() {

        /* Fill the contract list fragment */
        tendersListFragment = (ContractListFragment) viewPageFragment
                .pageAdapter.fragments.get(TENDER_FRAGMENT_INDEX);
        if (tendersListFragment != null) {
            tendersListFragment.setContracts(tenders);
            tendersListFragment.displayContracts();
        } else
            System.out.println("NULL contract list fragment");
    }


    /* Fil lthe list of companies for this institution */
    private void displayCompanies() {

        /* Fill the companies list fragment */
        companyListFragment = (CompanyListFragment) viewPageFragment
                .pageAdapter.fragments.get(INSTITUTIONS_FRAGMENT_INDEX);
        if (companyListFragment != null) {
            companyListFragment.setCompanies(companies);
            companyListFragment.displayCompanies();
        } else
            System.out.println("NULL company list fragment");
    }


    /* Fil lthe list of public institutions for a company */
    private void displayPIs() {

        /* Fill the companies list fragment */
        piListFragment = (InstitutionListFragment) viewPageFragment
                .pageAdapter.fragments.get(INSTITUTIONS_FRAGMENT_INDEX);
        if (piListFragment != null) {
            piListFragment.setPIs(pis);
            piListFragment.displayPIs();
        } else
            System.out.println("NULL pi list fragment");
    }

    @Override
    public void onStop() {
        ((HomeActivity) getActivity()).setActionBarTitle(oldTitle);
        super.onStop();
    }

    /**
     * Used to determine when a tab page is changed
     * @param position
     */
    @Override
    public void onPageChanged(int position) {
        System.out.println("InstitutionFragment: position has changed to " + position);

        getServerTabInfo(position);
    }
}
