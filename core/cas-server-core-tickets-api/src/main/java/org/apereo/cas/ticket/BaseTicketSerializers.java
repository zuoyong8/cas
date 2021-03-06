package org.apereo.cas.ticket;

import org.apereo.cas.ticket.proxy.ProxyGrantingTicket;
import org.apereo.cas.ticket.proxy.ProxyTicket;
import org.apereo.cas.ticket.registry.EncodedTicket;
import org.apereo.cas.util.function.FunctionUtils;
import org.apereo.cas.util.serialization.AbstractJacksonBackedStringSerializer;
import org.apereo.cas.util.serialization.StringSerializer;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.lambda.Unchecked;

import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is {@link BaseTicketSerializers}
 * that attempts to serialize ticket objects.
 *
 * @author Misagh Moayyed
 * @since 5.1.0
 */
@Slf4j
public abstract class BaseTicketSerializers {

    private static final Map<String, Class> TICKET_TYPE_CACHE = new ConcurrentHashMap<>();
    private static final PrettyPrinter MINIMAL_PRETTY_PRINTER = new MinimalPrettyPrinter();

    private static final StringSerializer<ProxyGrantingTicket> PROXY_GRANTING_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = 7089208351327601379L;

        @Override
        protected Class<ProxyGrantingTicket> getTypeToSerialize() {
            return ProxyGrantingTicket.class;
        }
    };

    private static final StringSerializer<ProxyTicket> PROXY_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = -6343596853082798477L;

        @Override
        protected Class<ProxyTicket> getTypeToSerialize() {
            return ProxyTicket.class;
        }
    };

    private static final StringSerializer<TicketGrantingTicket> TICKET_GRANTING_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = 1527874389457723545L;

        @Override
        protected Class<TicketGrantingTicket> getTypeToSerialize() {
            return TicketGrantingTicket.class;
        }
    };

    private static final StringSerializer<ServiceTicket> SERVICE_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = 8959617299162115085L;

        @Override
        protected Class<ServiceTicket> getTypeToSerialize() {
            return ServiceTicket.class;
        }
    };

    private static final StringSerializer<TransientSessionTicket> TRANSIENT_SESSION_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = 8959617299162115085L;

        @Override
        protected Class<TransientSessionTicket> getTypeToSerialize() {
            return TransientSessionTicket.class;
        }
    };

    private static final StringSerializer<EncodedTicket> ENCODED_TICKET_SERIALIZER = new AbstractJacksonBackedStringSerializer<>(MINIMAL_PRETTY_PRINTER) {
        private static final long serialVersionUID = 8959835299162115085L;

        @Override
        protected Class<EncodedTicket> getTypeToSerialize() {
            return EncodedTicket.class;
        }
    };

    public static StringSerializer<TransientSessionTicket> getTransientSessionTicketSerializer() {
        return TRANSIENT_SESSION_TICKET_SERIALIZER;
    }

    public static StringSerializer<ProxyGrantingTicket> getProxyGrantingTicketSerializer() {
        return PROXY_GRANTING_TICKET_SERIALIZER;
    }

    public static StringSerializer<ProxyTicket> getProxyTicketSerializer() {
        return PROXY_TICKET_SERIALIZER;
    }

    public static StringSerializer<TicketGrantingTicket> getTicketGrantingTicketSerializer() {
        return TICKET_GRANTING_TICKET_SERIALIZER;
    }

    public static StringSerializer<ServiceTicket> getServiceTicketSerializer() {
        return SERVICE_TICKET_SERIALIZER;
    }

    public static StringSerializer<EncodedTicket> getEncodedTicketSerializer() {
        return ENCODED_TICKET_SERIALIZER;
    }

    /**
     * Serialize ticket.
     *
     * @param ticket the ticket
     * @return the string
     */
    public static String serializeTicket(final Ticket ticket) {
        val writer = new StringWriter();
        if (ticket instanceof ProxyGrantingTicket) {
            getProxyGrantingTicketSerializer().to(writer, ProxyGrantingTicket.class.cast(ticket));
        } else if (ticket instanceof ProxyTicket) {
            getProxyTicketSerializer().to(writer, ProxyTicket.class.cast(ticket));
        } else if (ticket instanceof TicketGrantingTicket) {
            getTicketGrantingTicketSerializer().to(writer, TicketGrantingTicket.class.cast(ticket));
        } else if (ticket instanceof ServiceTicket) {
            getServiceTicketSerializer().to(writer, ServiceTicket.class.cast(ticket));
        } else if (ticket instanceof EncodedTicket) {
            getEncodedTicketSerializer().to(writer, EncodedTicket.class.cast(ticket));
        } else if (ticket instanceof TransientSessionTicket) {
            getTransientSessionTicketSerializer().to(writer, TransientSessionTicket.class.cast(ticket));
        } else {
            LOGGER.warn("Could not find serializer to marshal ticket [{}]. Ticket type may not be supported.", ticket.getId());
        }

        return writer.toString();
    }

    /**
     * Deserialize ticket.
     *
     * @param ticketContent the ticket id
     * @param type          the type
     * @return the ticket instance.
     */
    @SneakyThrows
    public static Ticket deserializeTicket(final String ticketContent, final String type) {
        if (StringUtils.isBlank(type)) {
            throw new InvalidTicketException("Invalid ticket type [blank] specified");
        }
        val clazz = FunctionUtils.doIf(TICKET_TYPE_CACHE.containsKey(type),
            () -> TICKET_TYPE_CACHE.get(type),
            Unchecked.supplier(() -> {
                val clz = Class.forName(type);
                TICKET_TYPE_CACHE.put(type, clz);
                return clz;
            }))
            .get();
        return deserializeTicket(ticketContent, (Class) clazz);
    }

    /**
     * Deserialize ticket.
     *
     * @param <T>           the type parameter
     * @param ticketContent the ticket id
     * @param clazz         the clazz
     * @return the ticket instance
     */
    public static <T extends Ticket> T deserializeTicket(final String ticketContent, final Class<T> clazz) {
        val deserializer = getTicketDeserializerFor(clazz);
        if (deserializer == null) {
            throw new IllegalArgumentException("Unable to find ticket deserializer for " + clazz.getSimpleName());
        }
        val ticket = deserializer.from(ticketContent);
        if (ticket == null) {
            throw new InvalidTicketException(clazz.getName());
        }
        if (!clazz.isAssignableFrom(ticket.getClass())) {
            throw new ClassCastException("Ticket [" + ticket.getId()
                + " is of type " + ticket.getClass()
                + " when we were expecting " + clazz);
        }
        return (T) ticket;
    }

    private static StringSerializer<? extends Ticket> getTicketDeserializerFor(final Class clazz) {
        if (TicketGrantingTicket.class.isAssignableFrom(clazz)) {
            return getTicketGrantingTicketSerializer();
        }
        if (ServiceTicket.class.isAssignableFrom(clazz)) {
            return getServiceTicketSerializer();
        }
        if (TransientSessionTicket.class.isAssignableFrom(clazz)) {
            return getTransientSessionTicketSerializer();
        }
        if (EncodedTicket.class.isAssignableFrom(clazz)) {
            return getEncodedTicketSerializer();
        }
        return null;
    }
}
