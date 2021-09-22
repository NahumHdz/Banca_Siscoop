package com.fenoreste.rest.Dao;

import DTO.CustomerAccountDTO;
import DTO.CustomerContactDetailsDTO;
import DTO.CustomerDetailsDTO;
import DTO.CustomerSearchDTO;
import DTO.ogsDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.Entidades.Auxiliares;
import com.fenoreste.rest.Entidades.AuxiliaresD;
import com.fenoreste.rest.Entidades.AuxiliaresPK;
import com.fenoreste.rest.Entidades.CuentasSiscoop;
import com.fenoreste.rest.Entidades.Persona;
import com.fenoreste.rest.Entidades.PersonasPK;
import com.fenoreste.rest.Entidades.Productos;
import com.fenoreste.rest.Entidades.validaciones_telefono_siscoop;
import com.fenoreste.rest.Util.Utilidades;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import java.util.ArrayList;
import java.util.Date;
import javax.persistence.EntityTransaction;

public abstract class FacadeCustomer<T> {

    private static EntityManagerFactory emf;

    List<Object[]> lista = null;

    Utilidades Util = new Utilidades();

    public FacadeCustomer(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }

    public List<CustomerSearchDTO> search(String ogs, String nombre, String appaterno) {
        EntityManager em = emf.createEntityManager();
        List<CustomerSearchDTO> listaC = new ArrayList<CustomerSearchDTO>();
        CustomerSearchDTO client = null;
        try {
            int o = 0, g = 0, s = 0;
            List<Persona> listaPersonas = new ArrayList<>();
            PersonasPK pk = null;
            Persona p = null;
            String customerId = "";

            String name = "", curp = "", taxId = "", customerType = "";
            Date birthDate = null;
            String sql = "";
            if (!ogs.equals("")) {
                ogsDTO id_ogs = Util.ogs(ogs);
                sql = "SELECT * FROM personas WHERE "
                        + " idorigen = " + id_ogs.getIdorigen() + " AND idgrupo = " + id_ogs.getIdgrupo() + " AND idsocio = " + id_ogs.getIdsocio()
                        + " AND idgrupo = 10";
                /*o = Integer.parseInt(ogs.substring(0, 6));
                g = Integer.parseInt(ogs.substring(6, 8));
                s = Integer.parseInt(ogs.substring(8, 14));
                pk = new PersonasPK(o, g, s);*/

            } else {
                sql = "SELECT * FROM personas WHERE UPPER(replace(nombre,' ','')) LIKE '%" + nombre + "%' AND UPPER(appaterno||apmaterno) LIKE '%" + appaterno + "%' AND idgrupo=10";

            }
            System.out.println("SQL:" + sql);
            Query queryPersonas = em.createNativeQuery(sql, Persona.class);
            listaPersonas = queryPersonas.getResultList();
            System.out.println("lista:" + listaPersonas);

            for (int i = 0; i < listaPersonas.size(); i++) {
                p = listaPersonas.get(i);
                customerId = String.format("%06d", p.getPersonasPK().getIdorigen()) + String.format("%02d", p.getPersonasPK().getIdgrupo()) + String.format("%06d", p.getPersonasPK().getIdsocio());
                name = p.getNombre() + " " + p.getAppaterno() + " " + p.getApmaterno();
                taxId = p.getCurp();
                birthDate = p.getFechanacimiento();
                if (p.getRazonSocial() == null) {
                    customerType = "individual";
                } else {
                    customerType = "grupal";
                }

                client = new CustomerSearchDTO(
                        customerId,
                        name,
                        taxId,
                        dateToString(birthDate).replace("/", "-"),
                        "individual");
                listaC.add(client);
            }

            return listaC;
        } catch (Exception e) {
            em.close();
            System.out.println("Error al buscar cliente:" + e.getMessage());

        } finally {
            em.close();
        }

        return null;
    }

    public CustomerDetailsDTO details(String ogs) {
        EntityManager em = emf.createEntityManager();
        List<CustomerDetailsDTO> listaC = new ArrayList<CustomerDetailsDTO>();
        CustomerDetailsDTO client = new CustomerDetailsDTO();
        try {
            int o = Integer.parseInt(ogs.substring(0, 6));
            int g = Integer.parseInt(ogs.substring(6, 8));
            int s = Integer.parseInt(ogs.substring(8, 14));
            PersonasPK pk = new PersonasPK(o, g, s);
            Persona p = em.find(Persona.class, pk);
            String name = "", customerType = "";
            name = p.getNombre() + " " + p.getAppaterno() + " " + p.getApmaterno();
            if (p.getRazonSocial() == null) {
                customerType = "individual";
            } else {
                customerType = "grupal";
            }
            client.setNationalId(p.getCurp());
            client.setBirthDate(p.getFechanacimiento().toString().replace("/", "-"));
            client.setCustomerId(ogs);
            client.setName(name);
            client.setCustomerType("individual");
            client.setTaxId(p.getCurp());
            return client;
        } catch (Exception e) {
            System.out.println("Error al buscar cliente:" + e.getMessage());
            em.close();
        }
        em.close();

        return null;
    }

    public List<CustomerContactDetailsDTO> ContactDetails(String ogs) {
        EntityManager em = emf.createEntityManager();
        Query query = null;
        List<Object[]> ListaObjetos = null;
        ogsDTO id_ogs = Util.ogs(ogs);
        String consulta = "SELECT CASE WHEN p.telefono != '' THEN p.telefono ELSE '0' END as phone,"
                + " CASE WHEN p.celular != '' THEN p.celular ELSE '0000000000' END as cellphone,"
                + " CASE WHEN p.email != '' THEN  p.email ELSE '0' END as email"
                + " FROM personas p WHERE "
                + " p.idorigen = " + id_ogs.getIdorigen() + " AND p.idgrupo = " + id_ogs.getIdgrupo() + " AND p.idsocio = " + id_ogs.getIdsocio();
        CustomerContactDetailsDTO contactsPhone = new CustomerContactDetailsDTO();
        CustomerContactDetailsDTO contactsCellphone = new CustomerContactDetailsDTO();
        CustomerContactDetailsDTO contactsEmail = new CustomerContactDetailsDTO();
        List<CustomerContactDetailsDTO> ListaContactos = new ArrayList<CustomerContactDetailsDTO>();

        try {
            int o = Integer.parseInt(ogs.substring(0, 6));
            int g = Integer.parseInt(ogs.substring(6, 8));
            int s = Integer.parseInt(ogs.substring(8, 14));
            PersonasPK pk = new PersonasPK(o, g, s);
            Persona p = em.find(Persona.class, pk);
            if (p.getTelefono() != null) {
                contactsPhone.setCustomerContactId(ogs);
                contactsPhone.setCustomerContactType("phone");
                contactsPhone.setPhoneNumber("521" + p.getTelefono());
                ListaContactos.add(contactsPhone);

            }
            if (p.getCelular() != null) {
                contactsCellphone.setCustomerContactId(ogs);
                contactsCellphone.setCustomerContactType("phone");
                contactsCellphone.setCellphoneNumber("521" + p.getCelular());
                ListaContactos.add(contactsCellphone);
            }
            if (p.getEmail() != null) {
                contactsEmail.setCustomerContactId(ogs);
                contactsEmail.setCustomerContactType("email");
                contactsEmail.setEmail(p.getEmail());
                ListaContactos.add(contactsEmail);
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error al obtener detalles del socio:" + e.getMessage());

        }
        em.close();

        return ListaContactos;
    }

    public List<CustomerAccountDTO> Accounts(String customerId) {
        EntityManager em = emf.createEntityManager();
        ogsDTO ogs = Util.ogs(customerId);
        Query query = null;
        String consulta = "SELECT * FROM auxiliares a INNER JOIN tipos_cuenta_siscoop tp USING(idproducto) WHERE "
                + " idorigen = " + ogs.getIdorigen() + " AND idgrupo = " + ogs.getIdgrupo() + " AND idsocio = " + ogs.getIdsocio() + " AND estatus = 2";
        CustomerAccountDTO producto = new CustomerAccountDTO();
        try {
            query = em.createNativeQuery(consulta, Auxiliares.class);
            List<Auxiliares> ListaProd = query.getResultList();
            String status = "";
            String accountType = "";
            Object[] arr = {};
            Object[] arr1 = {"relationCode", "SOW"};
            List<CustomerAccountDTO> listaDeCuentas = new ArrayList<CustomerAccountDTO>();

            for (int k = 0; k < 1; k++) {
                for (int i = 0; i < ListaProd.size(); i++) {
                    Auxiliares a = ListaProd.get(i);
                    System.out.println("IdproductoA:" + a.getAuxiliaresPK().getIdproducto());
                    try {
                        CuentasSiscoop tp = em.find(CuentasSiscoop.class, a.getAuxiliaresPK().getIdproducto());
                        accountType = String.valueOf(tp.getProducttypename().trim().toUpperCase());
                        if (accountType.contains("TIME")) {
                            accountType = "TIME";
                        }
                    } catch (Exception e) {
                        System.out.println("Error producido:" + e.getMessage());
                    }
                    if (a.getEstatus() == 2) {
                        status = "OPEN";
                    } else if (a.getEstatus() == 3) {
                        status = "CLOSED";
                    } else {
                        status = "INACTIVE";
                    }

                    String og = String.format("%06d", a.getIdorigen()) + String.format("%02d", a.getIdgrupo());
                    String s = String.format("%06d", a.getIdsocio());

                    String op = String.format("%06d", a.getAuxiliaresPK().getIdorigenp()) + String.format("%05d", a.getAuxiliaresPK().getIdproducto());
                    String aa = String.format("%08d", a.getAuxiliaresPK().getIdauxiliar());
                    System.out.println("opa:" + op + "," + aa);
                    String cadenaa = aa.substring(4, 8);
                    String cade = "******" + cadenaa;

                    producto = new CustomerAccountDTO(
                            op + aa,
                            op + aa,
                            cade,
                            accountType,
                            "MXN",
                            String.valueOf(a.getAuxiliaresPK().getIdproducto().toString()),
                            status,
                            arr,
                            arr1);
                    listaDeCuentas.add(producto);
                    accountType = "";
                }
            }
            return listaDeCuentas;

        } catch (Exception e) {
            em.close();
            System.out.println("Error al obtener cuentas:" + e.getMessage());
        } finally {
            em.close();
        }

        return null;
    }

    public boolean findCustomer(String ogs) {
        boolean bandera = false;
        EntityManager em = emf.createEntityManager();
        ogsDTO id_ogs = Util.ogs(ogs);
        try {
            Query query = em.createNativeQuery("SELECT * FROM personas WHERE "
                    + " p.idorigen = " + id_ogs.getIdorigen() + " AND p.idgrupo = " + id_ogs.getIdgrupo() + " AND p.idsocio = " + id_ogs.getIdsocio());
            if (query != null) {
                bandera = true;
            }
        } catch (Exception e) {
            cerrar();
        }
        return bandera;
    }

    public String validateSetContactDetails(String customerId, String phone1, String email) {
        EntityManager em = emf.createEntityManager();
        String CHAR_LOWER = "abcdefghijklmnopqrstuvwxyz";
        String CHAR_UPPER = CHAR_LOWER.toUpperCase();
        String NUMBER = "0123456789";
        ogsDTO ogs = Util.ogs(customerId);

        String DATA_FOR_RANDOM_STRING = CHAR_LOWER + CHAR_UPPER + NUMBER;
        SecureRandom random = new SecureRandom();
        char rndChar = 0;
        String cadena = "";
        for (int i = 0; i < 12; i++) {
            // 0-62 (exclusivo), retorno aleatorio 0-61
            int rndCharAt = random.nextInt(DATA_FOR_RANDOM_STRING.length());
            rndChar = DATA_FOR_RANDOM_STRING.charAt(rndCharAt);
            String c = String.valueOf(rndChar);
            cadena = cadena + c;
        }
        String mensaje = "";

        try {

            String validarDatos = "SELECT CASE WHEN telefono IS NOT NULL THEN telefono ELSE 'NO DATA' END "
                    + " CASE WHEN celular IS NOT NULL THEN celular ELSE 'NO DATA' END "
                    + " CASE WHEN email IS NOT NULL THEN email ELSE 'NO DATA' END"
                    + " WHERE idorigenp=" + ogs.getIdorigen()
                    + "       idgrupo=" + ogs.getIdgrupo()
                    + "       idsocio=" + ogs.getIdsocio();
            PersonasPK pk = new PersonasPK(ogs.getIdorigen(), ogs.getIdgrupo(), ogs.getIdsocio());
            Persona p = em.find(Persona.class, pk);

            if (p.getCelular().equals("")) {
                mensaje = mensaje + "SIN CELULAR";
            }
            if (p.getEmail().equals("")) {
                mensaje = mensaje + ",SIN EMAIL";
            }

            System.out.println("mensaje:" + mensaje);
            validaciones_telefono_siscoop validar_datos_contacto = new validaciones_telefono_siscoop();

            System.out.println("telefono:" + email.substring(3, 13));
            if (mensaje.equals("")) {
                validar_datos_contacto = em.find(validaciones_telefono_siscoop.class, customerId);
                if (validar_datos_contacto == null) {
                    if (p.getCelular().equals(phone1.substring(3, 13)) && p.getEmail().equals(email)) {
                        em.getTransaction().begin();
                        int registrosInsertados = em.createNativeQuery("INSERT INTO validaciones_telefonos_siscoop VALUES (?,?,?,?,?)")
                                .setParameter(1, cadena.toUpperCase())
                                .setParameter(2, customerId)
                                .setParameter(3, "521" + p.getCelular())
                                .setParameter(4, "521" + p.getCelular())
                                .setParameter(5, p.getEmail()).executeUpdate();
                        em.getTransaction().commit();
                        System.out.println("Registros insertados:" + registrosInsertados);
                        if (registrosInsertados > 0) {
                            mensaje = cadena;
                        }
                    } else {
                        mensaje = "Los datos no concuerdan con los de la base de datos";
                    }
                } else {
                    if (validar_datos_contacto.getCelular().equals(phone1) && validar_datos_contacto.getEmail().equals(email)) {
                        mensaje = validar_datos_contacto.getValidacion();
                    } else {
                        mensaje = "Ya existe un registro validado para:" + customerId + " pero no son los datos que se esta validando.";
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("Error en validar datos:" + e.getMessage());
            em.close();
        } finally {
            em.close();
        }

        return mensaje;
    }

    public String executeSetContactDetails(String validationId) {
        EntityManager em = emf.createEntityManager();
        String estatus = "";
        try {
            String consulta = "SELECT * FROM validaciones_telefonos_siscoop WHERE validacion='" + validationId + "'";
            System.out.println("consulta:" + consulta);
            Query query = em.createNativeQuery(consulta, validaciones_telefono_siscoop.class);
            validaciones_telefono_siscoop dto = (validaciones_telefono_siscoop) query.getSingleResult();
            if (dto != null) {
                estatus = "completed";
            }
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
        } finally {
            em.close();
        }

        return estatus;
    }

    public Double[] position(String customerId) {
        EntityManager em = emf.createEntityManager();
        ogsDTO ogs = Util.ogs(customerId);
        double saldo_congelado = 0.0;
        double saldo_disponible = 0.0;
        double saldo_disponible_actual = 0.0;
        try {

            String consulta_productos = "SELECT * FROM auxiliares a INNER JOIN tipos_cuenta_siscoop tp USING(idproducto) INNER JOIN productos p USING (idproducto)"
                    + " WHERE a.idorigen=" + ogs.getIdorigen()
                    + " AND idgrupo=" + ogs.getIdgrupo()
                    + " AND idsocio=" + ogs.getIdsocio()
                    + " AND a.estatus=2 AND tipoproducto in (0,1)";
            System.out.println("Consulta:" + consulta_productos);
            Query query = em.createNativeQuery(consulta_productos, Auxiliares.class);

            List<Auxiliares> lista_productos = query.getResultList();
            boolean bandera = false;
            for (int i = 0; i < lista_productos.size(); i++) {

                Auxiliares a = lista_productos.get(i);
                Productos pr = em.find(Productos.class, a.getAuxiliaresPK().getIdproducto());

                bandera = true;

                //Si es una inversion
                System.out.println("idproducto:" + a.getAuxiliaresPK().getIdproducto() + ",i:" + i + ",Saldo disponible:" + saldo_disponible + ",saldoCongelado:" + saldo_congelado);
                Query query_fecha_servidor = em.createNativeQuery("SELECT date(fechatrabajo) FROM origenes limit 1");
                String fecha_servidor = String.valueOf(query_fecha_servidor.getSingleResult());
                Date fecha_obtenida_servidor_db = stringToDate(fecha_servidor.replace("-", "/"));//fecha obtenida_servidor
                if (pr.getTipoproducto() == 1) {
                    saldo_disponible_actual = saldo_disponible_actual + a.getSaldo().doubleValue();
                    //Se suma fechaactivacion mas plazos para determinar si el producto ya se puede cobrar o aun no
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                    String fecha_auxiliar = dateFormat.format(a.getFechaactivacion());
                    String calcular_disponibilidad_saldo = "SELECT fechaactivacion + " + Integer.parseInt(String.valueOf(a.getPlazo())) + " FROM auxiliares a WHERE a.idorigenp="
                            + a.getAuxiliaresPK().getIdorigenp()
                            + " AND a.idproducto=" + a.getAuxiliaresPK().getIdproducto()
                            + " AND a.idauxiliar=" + a.getAuxiliaresPK().getIdauxiliar();
                    System.out.println("Calcular disponibilidad:" + calcular_disponibilidad_saldo);
                    Query fecha_disponibilidad_inversion = em.createNativeQuery(calcular_disponibilidad_saldo);
                    String fecha = String.valueOf(fecha_disponibilidad_inversion.getSingleResult()).replace("-", "/");

                    Date fecha_vencimiento_folio = stringToDate(fecha);//fecha vencimiento_folio_auxiliar

                    //si la fecha obtenida es igual al dia actual(hoy) o esta antes: El saldo de la inversion se puede retirar siempre y cuando no este amparando credito
                    //saldoLedgerDPF = saldoLedgerDPF + Double.parseDouble(a.getSaldo().toString());
                    System.out.println("fechaVencimientoFolio:" + fecha_vencimiento_folio);
                    System.out.println("FechaTrabajo:" + fecha_obtenida_servidor_db.toString());

                    if (fecha_vencimiento_folio.equals(fecha_obtenida_servidor_db) || fecha_vencimiento_folio.before(fecha_obtenida_servidor_db)) {
                        //Si ya esta disponible pero esta en garantia

                        if (a.getGarantia().intValue() > 0) {
                            saldo_congelado = saldo_congelado + Double.parseDouble(a.getGarantia().toString());
                            saldo_disponible = saldo_disponible + (Double.parseDouble(a.getSaldo().toString()) - Double.parseDouble(a.getGarantia().toString()));
                        } else {//Si ya se puede retirar la inversion por la fecha y no esta en garantia entonces el saldo ya esta disponible
                            saldo_disponible = saldo_disponible + Double.parseDouble(a.getSaldo().toString());
                        }
                        //Si el dpf aun no se puede retirar
                    } else {
                        saldo_congelado = saldo_congelado + Double.parseDouble(a.getSaldo().toString());
                    }

                } else if (pr.getTipoproducto() == 0) {
                    saldo_disponible_actual = saldo_disponible_actual + a.getSaldo().doubleValue();
                    if (pr.getNombre().toUpperCase().contains("NAVI")) {
                        String fecha = dateToString(fecha_obtenida_servidor_db);
                        if (fecha.substring(5, 7).contains("12")) {
                            if (Double.parseDouble(a.getGarantia().toString()) > 0) {
                                saldo_congelado = saldo_congelado + Double.parseDouble(a.getGarantia().toString());
                                saldo_disponible = saldo_disponible + (Double.parseDouble(a.getSaldo().toString()) - Double.parseDouble(a.getGarantia().toString()));

                            } else {
                                saldo_disponible = saldo_disponible + Double.parseDouble(a.getSaldo().toString());
                            }
                        } else {
                            saldo_congelado = saldo_congelado + a.getSaldo().doubleValue();
                        }
                    } else {
                        if (Double.parseDouble(a.getGarantia().toString()) > 0) {
                            saldo_congelado = saldo_congelado + Double.parseDouble(a.getGarantia().toString());
                            saldo_disponible = saldo_disponible + (Double.parseDouble(a.getSaldo().toString()) - Double.parseDouble(a.getGarantia().toString()));

                        } else {

                            saldo_disponible = saldo_disponible + Double.parseDouble(a.getSaldo().toString());
                        }
                    }
                }
                System.out.println("i:" + i + " ,saldo:" + a.getSaldo() + ",disponible:" + saldo_disponible + ", congelado:" + saldo_congelado);
            }

            System.out.println("El saldo disponible=" + saldo_disponible);
            System.out.println("El saldo congelado=" + saldo_congelado);
        } catch (Exception e) {
            e.getStackTrace();
            System.out.println("Error:" + e.getMessage());
            em.close();
        } finally {
            em.close();
        }
        Double saldos[] = new Double[2];
        saldos[0] = saldo_disponible;
        saldos[1] = saldo_disponible_actual;
        return saldos;
    }

    public Double[] positionHistory(String customerId, String fecha1, String fecha2) {
        EntityManager em = emf.createEntityManager();
        ogsDTO ogs = Util.ogs(customerId);
        Double ledGer = 0.0, avalaible = 0.0;
        try {
            String c = "SELECT * FROM auxiliares_d ad "
                    + "INNER JOIN auxiliares a using(idorigenp,idproducto,idauxiliar)"
                    + " INNER JOIN productos pr USING(idproducto)"
                    + " WHERE pr.tipoproducto in(0,1) "
                    + " AND a.idorigen = " + ogs.getIdorigen() + " AND a.idgrupo = " + ogs.getIdgrupo() + " AND a.idsocio = " + ogs.getIdsocio()
                    + " AND date(ad.fecha) between '" + fecha1 + "' AND '" + fecha2 + "'";
            System.out.println("Consulta:" + c);
            Query query = em.createNativeQuery(c, AuxiliaresD.class);
            List<AuxiliaresD> lista = query.getResultList();

            for (int i = 0; i < lista.size(); i++) {
                AuxiliaresD ad = lista.get(i);
                AuxiliaresPK apk = new AuxiliaresPK(ad.getAuxiliaresDPK().getIdorigenp(), ad.getAuxiliaresDPK().getIdproducto(), ad.getAuxiliaresDPK().getIdauxiliar());
                Auxiliares a = em.find(Auxiliares.class, apk);
                System.out.println("paso");
                Productos pr = em.find(Productos.class, a.getAuxiliaresPK().getIdproducto());
                Double garantiaDPF = 0.0;
                Double saldoAvalaibleDPF = 0.0;
                Double saldoLedgerDPF = 0.0;
                Double garantiaAhorro = 0.0;
                Double saldoAvalaibleAhorro = 0.0;
                Double saldoLedgerAhorro = 0.0;
                if (pr.getTipoproducto() == 1) {
                    //Se suma fechaactivacion mas plazos para determinar si el producto ya se puede cobrar o aun no
                    String cc = "SELECT a.fechaactivacion + " + Integer.parseInt(String.valueOf(a.getPlazo())) + " FROM auxiliares a WHERE a.idorigenp="
                            + a.getAuxiliaresPK().getIdorigenp()
                            + " AND a.idproducto=" + a.getAuxiliaresPK().getIdproducto()
                            + " AND a.idauxiliar=" + a.getAuxiliaresPK().getIdauxiliar();
                    Query query1 = em.createNativeQuery(cc);
                    String fecha = String.valueOf(query1.getSingleResult()).replace("-", "/");
                    Date hoy = new Date();
                    Date fa = stringToDate(fecha);//fecha obtenida
                    //si la fecha obtenida es igual al dia actual(hoy) o esta antes el dpf ya se puede retirar siempre y cuando no este amparando credito
                    saldoLedgerDPF = saldoLedgerDPF + Double.parseDouble(a.getSaldo().toString());
                    if (fa == hoy || fa.before(hoy)) {
                        //si el dpf tiene amparado credito
                        if (Integer.parseInt(a.getGarantia().toString()) > 0) {
                            garantiaDPF = garantiaDPF + Double.parseDouble(a.getGarantia().toString());
                            saldoAvalaibleDPF = saldoAvalaibleDPF + (Double.parseDouble(a.getSaldo().toString()) - garantiaDPF);
                        } else {
                            saldoAvalaibleDPF = Double.parseDouble(a.getSaldo().toString());
                        }
                        //Si el dpf aun no se puede retirar
                    }

                } else if (pr.getTipoproducto() == 0) {
                    saldoLedgerAhorro = saldoLedgerAhorro + Double.parseDouble(a.getSaldo().toString());
                    if (Double.parseDouble(a.getGarantia().toString()) > 0) {
                        garantiaAhorro = garantiaAhorro + Double.parseDouble(a.getGarantia().toString());
                        saldoAvalaibleAhorro = saldoAvalaibleAhorro + (Double.parseDouble(a.getSaldo().toString()) - garantiaAhorro);
                    } else {
                        saldoAvalaibleAhorro = saldoAvalaibleAhorro + Double.parseDouble(a.getSaldo().toString());
                    }
                }
                ledGer = ledGer + (saldoLedgerDPF + saldoLedgerAhorro);
                avalaible = avalaible + (saldoAvalaibleDPF + saldoAvalaibleAhorro);
            }

        } catch (Exception e) {
            e.getStackTrace();
            System.out.println("Error:" + e.getMessage());
        } finally {
            em.close();
        }
        Double saldos[] = new Double[2];
        saldos[0] = ledGer;
        saldos[1] = avalaible;
        System.out.println("Saldos:" + saldos[1]);
        return saldos;
    }

    public String dateToString(Date cadena) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String cadenaStr = sdf.format(cadena);
        return cadenaStr;
    }

    public Date stringToDate(String cadena) {
        SimpleDateFormat formato = new SimpleDateFormat("yyyy/MM/dd");
        Date fechaDate = null;

        try {
            fechaDate = formato.parse(cadena);
        } catch (Exception ex) {
            System.out.println("Error fecha:" + ex.getMessage());
        }
        System.out.println("fechaDate:" + fechaDate);
        return fechaDate;
    }

    public void cerrar() {
        emf.close();
    }

}
