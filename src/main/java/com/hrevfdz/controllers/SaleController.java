package com.hrevfdz.controllers;

import com.hrevfdz.dao.AccessDAO;
import com.hrevfdz.dao.SaleDAO;
import com.hrevfdz.dao.StockProductoDAO;
import com.hrevfdz.models.Access;
import com.hrevfdz.models.Sale;
import com.hrevfdz.models.StockProducto;
import com.hrevfdz.report.Conexion;
import com.hrevfdz.services.IPharmacy;
import com.hrevfdz.util.AccionUtil;
import com.hrevfdz.util.MessagesUtil;
import com.hrevfdz.util.QueriesUtil;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;

@ManagedBean
@ViewScoped
public class SaleController implements Serializable {

    private IPharmacy<Sale> dao;

    private List<Sale> sales;
    private Sale sale;
    private Sale tempSale;
    private List<StockProducto> stocks;
    private StockProducto producto;
    private String accion;
    private boolean estado;
    private Access access;

    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    private Date fecha = new Date();
    private String fecAct = sdf.format(fecha);

    @PostConstruct
    public void init() {
        try {
            sale = new Sale();
            tempSale = new Sale();
            doFindAll();
            doFindAllStock();
            doGetUserActive();
            sale.setFecha(sdf.parse(fecAct));
        } catch (ParseException ex) {
            Logger.getLogger(SaleController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void generarReporte() throws JRException, IOException, SQLException {
        try {
            SimpleDateFormat sdfr = new SimpleDateFormat("dd/MM/yyyy");
            fecha = sdfr.parse(sdfr.format(fecha));
            Map<String, Object> parametro = new HashMap<String, Object>();
            parametro.put("fec", fecha);

            File jasper = new File(FacesContext.getCurrentInstance().getExternalContext().getRealPath("/reports/ventas.jasper"));
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasper.getPath(), parametro, Conexion.getConexion());

            HttpServletResponse response = (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
            String filename = "Reporte de Ventas - (" + sdfr.format(fecha) + ").pdf";
            response.addHeader("Content-disposition", "attachment; filename=" + filename);
            try (ServletOutputStream stream = response.getOutputStream()) {
                JasperExportManager.exportReportToPdfStream(jasperPrint, stream);
                stream.flush();
            }
            FacesContext.getCurrentInstance().responseComplete();
        } catch (ParseException ex) {
            Logger.getLogger(SaleController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void doFindAll() {
        FacesMessage message = null;
        dao = new SaleDAO();

        try {
            final String query = "SELECT s FROM Sale s ORDER BY s.fecha DESC";
            sales = dao.findByQuery(query);
        } catch (Exception ex) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR EN DB", ex.getMessage());
        }

        if (message != null) {
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void doFindAllStock() {
        FacesMessage message = null;
        IPharmacy<StockProducto> daoS = new StockProductoDAO();

        try {
            stocks = daoS.findAll();
        } catch (Exception ex) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR EN DB", ex.getMessage());
        }

        if (message != null) {
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void doFindStockByCod() {
        FacesMessage message = null;
        IPharmacy<StockProducto> daoS = new StockProductoDAO();

        try {
            List<StockProducto> lista = daoS
                    .findByQuery(QueriesUtil.STOCK_X_COD + tempSale.getCodStock().getCodStock());
            if (lista.size() == 1) {
                for (StockProducto sp : lista) {
                    producto = sp;
                }
            }
        } catch (Exception ex) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR EN DB", ex.getMessage());
        }

        if (message != null) {
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public void doFindByFecha() {
        FacesMessage message = null;
        dao = new SaleDAO();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        final String query = "SELECT s FROM Sale s WHERE s.fecha = '" + sdf.format(fecha) + "'";

        try {
            if (fecha != null) {
                sales.clear();
                sales = dao.findByQuery(query);
            }
        } catch (Exception ex) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR EN DB", ex.getMessage());
        }

        if (message != null) {
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }

    public double doGetTotal(Date f) {
        FacesMessage message = null;
        IPharmacy daos = new SaleDAO();
        SimpleDateFormat sdf4 = new SimpleDateFormat("yyyy-MM-dd");

        String query = "SELECT SUM(s.subtotal) FROM Sale s WHERE s.fecha = '";

        double total = 0;

        try {
//            query += (fecha != null) ? sdf4.format(fecha) : sdf4.format(f);
            query += sdf4.format(f);
            query += "'";
            total = daos.findBy(query) != null ? (double) daos.findBy(query) : 0;
        } catch (Exception ex) {
            message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR EN DB", ex.getMessage());
        }

        if (message != null) {
            FacesContext.getCurrentInstance().addMessage(null, message);
        }

        return total;
    }

    public void calcSubtotal() {
        double c = sale.getCantidad();
        double p = producto.getMonto();
        sale.setSubtotal(p * c);
    }

    public void doCreate() {
        FacesMessage msg = null;
        StockProductoDAO daoSt = new StockProductoDAO();
        boolean result = false;
        boolean resultST = false;

        try {
            sale.setPrecio(producto.getMonto());
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date fec = new Date();
            sale.setHora(sdf.parse(sdf.format(fec)));

            sale.setCodStock(producto);

            List<StockProducto> sps = daoSt.findByQuery(QueriesUtil.STOCK_X_COD + producto.getCodStock());
            StockProducto tempSt = new StockProducto();

            if (sps.size() == 1) {
                for (StockProducto sp : sps) {
                    tempSt = sp;
                }

                if (tempSt.getCantidad() >= sale.getCantidad()) {
                    result = dao.Create(sale);

                    if (result) {
                        producto.setCantidad(tempSt.getCantidad() - sale.getCantidad());
                        resultST = daoSt.Update(producto);
                    }

                    if (result && resultST) {
                        sales.add(sales.size(), sale);
                        doFindAll();
                        msg = new FacesMessage(FacesMessage.SEVERITY_INFO, MessagesUtil.SUCCESS_TITLE, MessagesUtil.SAVE_SUCCESS);
                    } else {
                        msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, MessagesUtil.FAIL_TITLE, MessagesUtil.SAVE_FAIL);
                    }
                } else {
                    msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Stock insuficiante",
                            "No cuenta con stock para realizar la venta");
                    FacesContext.getCurrentInstance().addMessage(null, msg);
                }
            }
        } catch (Exception e) {
            msg = new FacesMessage(FacesMessage.SEVERITY_FATAL, MessagesUtil.ERROR_TITLE, e.getMessage());
        }

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doUpdate(Sale s) {
        FacesMessage msg = null;
        dao = new SaleDAO();
        StockProductoDAO daoSt = new StockProductoDAO();
        boolean result = false;
        boolean resultST = false;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            Date fec = new Date();
            sale.setHora(sdf.parse(sdf.format(fec)));

            sale.setCodStock(producto);

            List<StockProducto> sps = daoSt.findByQuery(QueriesUtil.STOCK_X_COD + producto.getCodStock());
            List<Sale> ses = dao.findByQuery(QueriesUtil.SALE_X_COD + sale.getCodSale());
            StockProducto tempSt = new StockProducto();
            Sale tempVenta = new Sale();

            if (sps.size() == 1) {
                for (StockProducto sp : sps) {
                    tempSt = sp;
                }

                for (Sale se : ses) {
                    tempVenta = se;
                }

                if (tempSt.getCantidad() >= sale.getCantidad()) {
                    result = dao.Update(sale);
                    int venta = 0;
                    if (tempVenta.getCantidad() > sale.getCantidad()) {
                        venta = tempVenta.getCantidad() - sale.getCantidad();
                        producto.setCantidad(producto.getCantidad() + venta);
                    } else {
                        venta = sale.getCantidad() - tempVenta.getCantidad();
                        producto.setCantidad(producto.getCantidad() - venta);
                    }

                    resultST = daoSt.Update(producto);

                    if (result && resultST) {
                        sales.clear();
                        doFindAll();
                        sale = new Sale();
                        msg = new FacesMessage(FacesMessage.SEVERITY_INFO, MessagesUtil.SUCCESS_TITLE, MessagesUtil.SAVE_SUCCESS);
                    } else {
                        msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, MessagesUtil.FAIL_TITLE, MessagesUtil.UPDATE_FAIL);
                    }
                } else {
                    msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Stock insuficiante",
                            "No cuenta con stock para realizar la venta");
                    FacesContext.getCurrentInstance().addMessage(null, msg);
                }

            }

        } catch (Exception e) {
            msg = new FacesMessage(FacesMessage.SEVERITY_FATAL, MessagesUtil.ERROR_TITLE, e.getMessage());
        }

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void doGetUserActive() {
        FacesMessage msg = null;
        IPharmacy<Access> dao = new AccessDAO();

        try {
            final String query = "SELECT a FROM Access a WHERE a.id = (SELECT MAX(t.id) FROM Access t)";
            access = dao.findBy(query);
            this.sale.setUserId(access.getUserId());
        } catch (Exception e) {
            msg = new FacesMessage(FacesMessage.SEVERITY_INFO, MessagesUtil.ERROR_TITLE, e.getMessage());
        }

        if (msg != null) {
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public void doDelete(Sale s) {
        FacesMessage msg = null;
        dao = new SaleDAO();
        StockProductoDAO daoSt = new StockProductoDAO();

        boolean result = false;
        boolean resultST = false;

        try {
            StockProducto sp = daoSt.findBy(QueriesUtil.STOCK_X_COD + producto.getCodStock());
            producto.setCantidad(producto.getCantidad() + sale.getCantidad());

            resultST = daoSt.Update(producto);

            if (!resultST) {
                msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, MessagesUtil.ERROR_TITLE, "No se pudo reponer el Stock");
            }

            result = dao.Delete(s);

            if (result) {
                sales.clear();
                doFindAll();
                sale = new Sale();
                msg = new FacesMessage(FacesMessage.SEVERITY_INFO, MessagesUtil.SUCCESS_TITLE, MessagesUtil.DELETE_SUCCESS);
            } else {
                msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, MessagesUtil.FAIL_TITLE, MessagesUtil.DELETE_FAIL);
            }
        } catch (Exception e) {
            msg = new FacesMessage(FacesMessage.SEVERITY_FATAL, MessagesUtil.ERROR_TITLE, e.getMessage());
        }

        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public TimeZone getTimeZone() {
        TimeZone timeZone = TimeZone.getDefault();
        return timeZone;
    }

    public void doNew() {
        accion = AccionUtil.CREATE;
        sale = new Sale();
        doFindAllStock();
        doGetUserActive();
        estado = false;
        try {
            sale.setFecha(sdf.parse(fecAct));
        } catch (ParseException ex) {
            Logger.getLogger(SaleController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void doUpgrade(Sale s) {
        accion = AccionUtil.UPDATE;
        sale = s;
        doFindAllStock();
        tempSale = sale;
        doFindStockByCod();
        doGetUserActive();
        estado = true;
    }

    public void doExecute() {
        switch (accion) {
            case AccionUtil.CREATE:
                doCreate();
                break;
            case AccionUtil.UPDATE:
                doUpdate(sale);
                break;
        }
    }

    public List<Sale> getSales() {
        return sales;
    }

    public void setSales(List<Sale> sales) {
        this.sales = sales;
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public List<StockProducto> getStocks() {
        return stocks;
    }

    public void setStocks(List<StockProducto> stocks) {
        this.stocks = stocks;
    }

    public StockProducto getProducto() {
        return producto;
    }

    public void setProducto(StockProducto producto) {
        this.producto = producto;
    }

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Sale getTempSale() {
        return tempSale;
    }

    public void setTempSale(Sale tempSale) {
        this.tempSale = tempSale;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public String getFecAct() {
        return fecAct;
    }

    public void setFecAct(String fecAct) {
        this.fecAct = fecAct;
    }

}
