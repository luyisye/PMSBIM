package weaver.pmsbim.eric.action;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.weaver.general.Util;

import weaver.conn.RecordSet;
import weaver.interfaces.workflow.action.Action;
import weaver.pmsbim.eric.util.TableDataUtil;
import weaver.pmsbim.eric.util.XmlUtil;
import weaver.soa.workflow.request.RequestInfo;
/**
 * ����Ӫ�ѷ��ñ��������̽��������ݸ�NC
 * 
 * @author tengfei.ye@weaver.cn
 * @version 2019/7/18 17:00
 */
public class Jyfybx2NCAction implements Action{
	private Log log = LogFactory.getLog(Jyfybx2NCAction.class.getName());
	String requestid = "";
	String billid = "";
	@Override
	public String execute(RequestInfo requestInfo) {
		try {
            log.info("���롶��Ӫ���ñ��������̽�����ֵ��NC����...");
            requestid = requestInfo.getRequestid();
            billid=requestInfo.getRequestManager().getBillid()+"";
            Map<String, String> clfMainMap = TableDataUtil.getMainMap(log, requestInfo);// ����
            //List<Map<String,String>> listMap1 = TableDataUtil.getDtListMap(log, requestInfo, 0);// ��ϸ��1
            // ����NC-WebService�ӿ�
            String xml = getJyfyXml(clfMainMap);// �ӿڲ���
            log.info("xml:" + xml);
            weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator locator = new weaver.pmsbim.eric.ncWebServiceClient.PMWebserviceLocator();
            weaver.pmsbim.eric.ncWebServiceClient.PMWebservicePortType service = locator.getPMWebserviceSOAP11port_http();
            String result = service.voucherOrder(xml, "", "", "", "", "", "", "", "", "", "");
            String key = " successful=\"";// NC�ӿڷ���ֵ��key��[ successful="]
            int index = result.indexOf(key);// key�ڷ���ֵ�ַ����г��ֵĵ�һ�ε��±�
            String successRst = result.substring(index + key.length(), index + key.length() + 1);// ���ص�successful����ֵ��Y-�ɹ���N-ʧ��
            Map<String, String> contentMap = XmlUtil.xmlToMap(result, "UTF-8");
            if (successRst.equals("Y")) {
                log.info("�ӿڵ��óɹ�������ֵΪ��\n" + result);
            } else {
                log.info("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
                requestInfo.getRequestManager().setMessagecontent("�ӿڵ���ʧ�ܣ�����ֵΪ��\n" + result);
                return "0";
            }
        } catch (Exception e) {
            log.error(e);
        }

        return SUCCESS;
    }

	/**
	 * ��ȡ���ñ���XML
	 * 
	 * @param jyfyMainMap ����Ӫ�ѷ��ñ�������������
	 * @return NC�涨�Ľӿ���������
	 * @author tengfei.ye@weaver.cn
	 * @version 2019/7/18 17:00
	 */
	private String getJyfyXml(Map<String, String> jyfyMainMap) {
		StringBuffer xmlSb = new StringBuffer();
		xmlSb.append("<?xml version='1.0' encoding='UTF-8'?>");
		xmlSb.append("<ufinterface billtype='voucher' msg='OA����NC��һ�����ñ�����'>");
		xmlSb.append("<bill>");
		xmlSb.append("<head>");
		xmlSb.append("<pk_m_oa>").append(requestid).append("</pk_m_oa>");// OA���̱��
		xmlSb.append("<pk_org>").append("806").append("</pk_org>");// ��˾��֯
		xmlSb.append("<vouchertype>").append("����ƾ֤").append("</vouchertype>");// ƾ֤��𣺹̶�ֵ ����ƾ֤
		xmlSb.append("<dbilldate>").append(jyfyMainMap.get("sqrq")).append("</dbilldate>");// ƾ֤����
		xmlSb.append("<billmaker>").append("hlj").append("</billmaker>");// �Ƶ��˱��� ��ʱֻ��ʹ��hlj
		if (jyfyMainMap.get("xgfj") != "") {
			int fjsl = jyfyMainMap.get("xgfj").split(",").length;// ��������
			xmlSb.append("<attachment>").append(fjsl).append("</attachment>");// ������
		} else {
			xmlSb.append("<attachment>").append("0").append("</attachment>");// ������
		}
		xmlSb.append("</head>");
		xmlSb.append("<body>");
		double sumjfje = 0.00;// �跽���ϼ�
		int inid = 1;
		String sql = "select *,ifnull(sum(rzje),0) as rzjehj from formtable_main_35_dt1 where mainid="+billid+" group by jfkm,fzhsbm,fycdbm,zy";//����ժҪ���跽��Ŀ������������Ŀ�����óе����ŷ���
		String sqlmain = "select * from formtable_main_35 where requestid="+requestid+"";
		RecordSet rs = new RecordSet();
		RecordSet rs2 = new RecordSet();
		rs.execute(sql);
		rs2.execute(sqlmain);
		while(rs.next()) {
			//sumjfje+=Util.getDoubleValue(rs.getString("rzje"),0.00);
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append(rs.getString("jfkm")).append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(rs.getString("rzjehj")).append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			if (!rs.getString("fzhsbm").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
				xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
				xmlSb.append("</item>");
			}
			if (!rs.getString("yfxm").equals("")) {
				xmlSb.append("<item>");
				xmlSb.append("<pk_Checktype>").append("�з���Ŀ").append("</pk_Checktype>");// ��Ŀ������������
				xmlSb.append("<pk_Checkvalue>").append(getXMBM(rs.getString("yfxm"))).append("</pk_Checkvalue>");// ��Ŀ�����������
				xmlSb.append("</item>");
			}
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			inid++;
		}
		//����˰22211601
		String rzcy = jyfyMainMap.get("rzcyhj");//���˲���
		log.info("rzcy:"+rzcy);
		String bxdj = jyfyMainMap.get("bxdj");//��������
		double je = Util.getDoubleValue(rzcy,0.00)-Util.getDoubleValue(bxdj,0.00);
		if(je!=0) {//����˰��Ϊ0���ܴ�
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("22211601").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append(je).append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append("0.00").append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		// ������Ŀ��ƾ֤
		boolean flag=false;
		if(jyfyMainMap.get("hjje")!="") {//�������Ϊ��
			xmlSb.append("<detail>");
			xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// �к�
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("1002").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(jyfyMainMap.get("hjje"))).append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("�����˻�").append("</pk_Checktype>");// ������������
			xmlSb.append("<pk_Checkvalue>").append("33001616781059888999").append("</pk_Checkvalue>");// �����������
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
			flag=true;
		}
		if(jyfyMainMap.get("cdbyj")!="") {//�����ֽ�Ϊ��
			xmlSb.append("<detail>");
			if(flag) {
				xmlSb.append("<detailindex>").append(inid+2).append("</detailindex>");// �к�
			}else {
				xmlSb.append("<detailindex>").append(inid+1).append("</detailindex>");// �к�
			}		
			xmlSb.append("<explanation>").append(rs.getString("zy")).append("</explanation>");// ժҪ
			xmlSb.append("<accsubjcode>").append("122102").append("</accsubjcode>");// ��Ŀ����
			xmlSb.append("<pk_currtype>").append("CNY").append("</pk_currtype>");// ����
			xmlSb.append("<excrate2>").append("1.000000").append("</excrate2>");// �۱�����
			xmlSb.append("<debitamount>").append("0.00").append("</debitamount>");// �跽���
			xmlSb.append("<creditamount>").append(Util.getDoubleValue(jyfyMainMap.get("cdbyj"))).append("</creditamount>");// �������
			xmlSb.append("<ass>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("��Ա����").append("</pk_Checktype>");// ��Ա����������������
			xmlSb.append("<pk_Checkvalue>").append(rs2.getString("fzhsry")).append("</pk_Checkvalue>");// ��Ա���������������
			xmlSb.append("</item>");
			xmlSb.append("<item>");
			xmlSb.append("<pk_Checktype>").append("����").append("</pk_Checktype>");// ���Ÿ�����������
			xmlSb.append("<pk_Checkvalue>").append(rs.getString("fzhsbm")).append("</pk_Checkvalue>");// ���Ÿ����������
			xmlSb.append("</item>");
			xmlSb.append("</ass>");
			xmlSb.append("</detail>");
		}
		//---------------------------------------------------------
		xmlSb.append("</body>");
		xmlSb.append("</bill>");
		xmlSb.append("</ufinterface>");
		return xmlSb.toString();
	}

	/* ��ȡ�з���Ŀ���� */
	public String getXMBM(String id) {
		String bm="";
		RecordSet rs = new RecordSet();
		String sql="select * from uf_yfxm where id="+id;
		rs.execute(sql);
		if(rs.next()) {
			bm=rs.getString("yfxmbm");
		}
		return bm;
	}
}
